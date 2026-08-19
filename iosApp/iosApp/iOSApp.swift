import SwiftUI
import UIKit
import UserNotifications
import FirebaseCore
import FirebaseAuth
import Shared

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        IosFirebaseAuthBridge.shared.host = FirebasePhoneOtpHost()
        IosRazorpayBridge.shared.host = RazorpayWebCheckoutHost()
        UNUserNotificationCenter.current().delegate = self
        application.registerForRemoteNotifications()
        return true
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Auth.auth().setAPNSToken(deviceToken, type: .unknown)
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        if Auth.auth().canHandleNotification(userInfo) {
            completionHandler(.noData)
            return
        }
        completionHandler(.noData)
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        Auth.auth().canHandle(url)
    }
}

final class PhoneAuthUIDelegate: NSObject, AuthUIDelegate {
    func present(_ viewControllerToPresent: UIViewController, animated flag: Bool, completion: (() -> Void)? = nil) {
        topViewController()?.present(viewControllerToPresent, animated: flag, completion: completion)
    }

    func dismiss(animated flag: Bool, completion: (() -> Void)? = nil) {
        topViewController()?.dismiss(animated: flag, completion: completion)
    }

    private func topViewController() -> UIViewController? {
        let root = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
        var top = root
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}

final class FirebasePhoneOtpHost: IosPhoneOtpHost {
    private let uiDelegate = PhoneAuthUIDelegate()

    func sendOtp(e164Phone: String, callback: IosOtpSendCallback) {
        PhoneAuthProvider.provider().verifyPhoneNumber(e164Phone, uiDelegate: uiDelegate) { verificationID, error in
            if let verificationID {
                callback.onSuccess(verificationId: verificationID)
            } else {
                callback.onFailure(message: error?.localizedDescription ?? "Could not send OTP")
            }
        }
    }

    func verifyOtp(verificationId: String, code: String, callback: IosOtpVerifyCallback) {
        let credential = PhoneAuthProvider.provider().credential(
            withVerificationID: verificationId,
            verificationCode: code
        )
        Auth.auth().signIn(with: credential) { result, error in
            if let error {
                callback.onFailure(message: error.localizedDescription)
                return
            }
            result?.user.getIDToken { token, tokenError in
                if let token {
                    callback.onSuccess(idToken: token)
                } else {
                    callback.onFailure(message: tokenError?.localizedDescription ?? "Could not verify OTP")
                }
            }
        }
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    _ = Auth.auth().canHandle(url)
                }
        }
    }
}
