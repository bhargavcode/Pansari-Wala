import SwiftUI
import UIKit
import Translation
import Shared

/// On-device English → app-language translation with Apple's Translation framework.
/// A language downloads once (system prompt, needs internet); afterwards it works offline.
final class AppleTranslatorHost: NSObject, IosTranslatorHost {
    private let model = TranslationQueue()
    private var hostController: UIViewController?

    func supportedLanguages(languageCodes: [String], onResult: @escaping ([String]) -> Void) {
        Task {
            let availability = LanguageAvailability()
            var supported: [String] = []
            for code in languageCodes {
                let status = await availability.status(
                    from: Locale.Language(identifier: TranslationQueue.sourceLanguage),
                    to: Locale.Language(identifier: code)
                )
                if status != .unsupported { supported.append(code) }
            }
            onResult(supported)
        }
    }

    func translate(text: String, languageCode: String, onResult: @escaping (String?) -> Void) {
        DispatchQueue.main.async {
            guard self.attachIfNeeded() else {
                onResult(nil)
                return
            }
            self.model.enqueue(TranslationJob(text: text, target: languageCode, onResult: onResult))
        }
    }

    /// `translationTask` only runs inside a live SwiftUI view, so keep a 1×1 host in the window.
    private func attachIfNeeded() -> Bool {
        if hostController?.view.window != nil { return true }
        guard let root = keyWindowRoot() else { return false }
        let controller = UIHostingController(rootView: TranslationHostView(queue: model))
        controller.view.frame = CGRect(x: 0, y: 0, width: 1, height: 1)
        controller.view.backgroundColor = .clear
        controller.view.isUserInteractionEnabled = false
        root.addChild(controller)
        root.view.addSubview(controller.view)
        controller.didMove(toParent: root)
        hostController = controller
        return true
    }

    private func keyWindowRoot() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
            .first { $0.isKeyWindow }?
            .rootViewController
    }
}

struct TranslationJob {
    let text: String
    let target: String
    let onResult: (String?) -> Void
}

/// Runs jobs one language at a time: each language change swaps the session configuration.
final class TranslationQueue: ObservableObject {
    static let sourceLanguage = "en"

    @Published var configuration: TranslationSession.Configuration?
    private var pending: [TranslationJob] = []
    private var running = false

    func enqueue(_ job: TranslationJob) {
        pending.append(job)
        startNextIfIdle()
    }

    private func startNextIfIdle() {
        guard !running, let next = pending.first else { return }
        running = true
        let target = Locale.Language(identifier: next.target)
        if configuration?.target == target {
            configuration?.invalidate()
        } else {
            configuration = TranslationSession.Configuration(
                source: Locale.Language(identifier: Self.sourceLanguage),
                target: target
            )
        }
    }

    @MainActor
    func run(_ session: TranslationSession) async {
        let target = configuration?.target
        while let job = pending.first, Locale.Language(identifier: job.target) == target {
            pending.removeFirst()
            let translated = try? await session.translate(job.text).targetText
            job.onResult(translated)
        }
        running = false
        startNextIfIdle()
    }
}

private struct TranslationHostView: View {
    @ObservedObject var queue: TranslationQueue

    var body: some View {
        Color.clear
            .frame(width: 1, height: 1)
            .translationTask(queue.configuration) { session in
                await queue.run(session)
            }
    }
}
