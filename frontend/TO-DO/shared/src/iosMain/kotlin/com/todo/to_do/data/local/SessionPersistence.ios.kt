package com.todo.to_do.data.local

import com.todo.to_do.domain.model.AuthSession
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFTypeRefVar
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * Stores the session in the iOS Keychain (not NSUserDefaults), so the bearer token is
 * encrypted at rest and excluded from unencrypted device/iCloud backups.
 */
@OptIn(ExperimentalForeignApi::class)
actual class SessionPersistence {
    actual fun save(session: AuthSession) {
        Keychain.set(KEY_TOKEN, session.token)
        Keychain.set(KEY_USER_ID, session.userId)
        Keychain.set(KEY_EMAIL, session.email)
    }

    actual fun load(): AuthSession? {
        val token = Keychain.get(KEY_TOKEN) ?: return null
        val userId = Keychain.get(KEY_USER_ID) ?: return null
        val email = Keychain.get(KEY_EMAIL) ?: return null
        return AuthSession(token = token, userId = userId, email = email)
    }

    actual fun clear() {
        Keychain.delete(KEY_TOKEN)
        Keychain.delete(KEY_USER_ID)
        Keychain.delete(KEY_EMAIL)
    }

    private companion object {
        const val KEY_TOKEN = "task_flow_session_token"
        const val KEY_USER_ID = "task_flow_session_user_id"
        const val KEY_EMAIL = "task_flow_session_email"
    }
}

// CFStringRef/NSString and NSMutableDictionary/CFDictionaryRef are toll-free bridged on Apple
// platforms, so these "as" casts are safe at runtime even though the K2 compiler cannot verify
// the bridging statically and warns CAST_NEVER_SUCCEEDS on every one of them.
@Suppress("CAST_NEVER_SUCCEEDS")
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private object Keychain {
    private const val SERVICE = "com.todo.to_do.session"

    fun set(account: String, value: String) {
        delete(account)
        val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val query = baseQuery(account).apply {
            setObject(data, forKey = kSecValueData as NSString)
            setObject(
                kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly as NSString,
                forKey = kSecAttrAccessible as NSString
            )
        }
        SecItemAdd(query as CFDictionaryRef, null)
    }

    fun get(account: String): String? {
        val query = baseQuery(account).apply {
            setObject(true, forKey = kSecReturnData as NSString)
            setObject(kSecMatchLimitOne as NSString, forKey = kSecMatchLimit as NSString)
        }
        return memScoped {
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr)
            if (status != errSecSuccess) return@memScoped null
            val data = result.value as? NSData ?: return@memScoped null
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
        }
    }

    fun delete(account: String) {
        SecItemDelete(baseQuery(account) as CFDictionaryRef)
    }

    private fun baseQuery(account: String): NSMutableDictionary =
        NSMutableDictionary().apply {
            setObject(kSecClassGenericPassword as NSString, forKey = kSecClass as NSString)
            setObject(SERVICE, forKey = kSecAttrService as NSString)
            setObject(account, forKey = kSecAttrAccount as NSString)
        }
}