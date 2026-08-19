import UIKit
import WebKit
import Shared

final class RazorpayWebCheckoutHost: NSObject, IosRazorpayHost, WKScriptMessageHandler, WKNavigationDelegate {
    private var callback: IosRazorpayCallback?
    private var webController: UIViewController?
    private var webView: WKWebView?
    private var completed = false

    func pay(
        keyId: String,
        orderId: String,
        amountPaise: Int64,
        currency: String,
        merchantName: String,
        customerName: String,
        customerPhone: String,
        description: String,
        callback: IosRazorpayCallback
    ) {
        self.callback = callback
        completed = false
        DispatchQueue.main.async {
            self.presentCheckout(
                keyId: keyId,
                orderId: orderId,
                amountPaise: amountPaise,
                currency: currency,
                merchantName: merchantName,
                customerName: customerName,
                customerPhone: customerPhone,
                description: description
            )
        }
    }

    func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
        guard message.name == "rzp", let body = message.body as? [String: Any] else { return }
        let status = body["status"] as? String
        if status == "success" {
            finishSuccess(
                paymentId: body["paymentId"] as? String ?? "",
                orderId: body["orderId"] as? String ?? "",
                signature: body["signature"] as? String ?? ""
            )
        } else if status == "cancel" {
            finishCancelled()
        } else {
            finishFailed()
        }
    }

    func webView(
        _ webView: WKWebView,
        decidePolicyFor navigationAction: WKNavigationAction,
        decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
    ) {
        guard let url = navigationAction.request.url, let scheme = url.scheme?.lowercased() else {
            decisionHandler(.allow)
            return
        }
        if scheme == "http" || scheme == "https" || scheme == "about" {
            decisionHandler(.allow)
            return
        }
        UIApplication.shared.open(url)
        decisionHandler(.cancel)
    }

    private func presentCheckout(
        keyId: String,
        orderId: String,
        amountPaise: Int64,
        currency: String,
        merchantName: String,
        customerName: String,
        customerPhone: String,
        description: String
    ) {
        guard let presenter = topViewController() else {
            finishUnavailable()
            return
        }
        var prefill: [String: Any] = [
            "name": customerName,
            "contact": customerPhone,
        ]
        if keyId.hasPrefix("rzp_test_") {
            prefill["method"] = "upi"
            prefill["vpa"] = "success@razorpay"
        }
        let options: [String: Any] = [
            "key": keyId,
            "amount": amountPaise,
            "currency": currency,
            "name": merchantName,
            "description": description,
            "order_id": orderId,
            "prefill": prefill,
        ]
        guard let jsonData = try? JSONSerialization.data(withJSONObject: options),
              let json = String(data: jsonData, encoding: .utf8) else {
            finishUnavailable()
            return
        }

        let html = """
        <!DOCTYPE html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <script src="https://checkout.razorpay.com/v1/checkout.js"></script>
        </head><body>
        <script>
        var options = \(json);
        options.handler = function(response) {
          window.webkit.messageHandlers.rzp.postMessage({
            status: "success",
            paymentId: response.razorpay_payment_id || "",
            orderId: response.razorpay_order_id || "",
            signature: response.razorpay_signature || ""
          });
        };
        options.modal = {
          ondismiss: function() {
            window.webkit.messageHandlers.rzp.postMessage({ status: "cancel" });
          }
        };
        var rzp = new Razorpay(options);
        rzp.on("payment.failed", function() {
          window.webkit.messageHandlers.rzp.postMessage({ status: "failed" });
        });
        rzp.open();
        </script>
        </body></html>
        """

        let content = WKUserContentController()
        content.add(self, name: "rzp")
        let config = WKWebViewConfiguration()
        config.userContentController = content
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.navigationDelegate = self
        webView.loadHTMLString(html, baseURL: URL(string: "https://api.razorpay.com"))
        self.webView = webView

        let controller = UIViewController()
        controller.view = webView
        controller.modalPresentationStyle = .overFullScreen
        webController = controller
        presenter.present(controller, animated: true)
    }

    private func finishSuccess(paymentId: String, orderId: String, signature: String) {
        guard !completed else { return }
        completed = true
        let cb = callback
        dismiss {
            cb?.onSuccess(paymentId: paymentId, orderId: orderId, signature: signature)
        }
    }

    private func finishCancelled() {
        guard !completed else { return }
        completed = true
        let cb = callback
        dismiss {
            cb?.onCancelled()
        }
    }

    private func finishFailed() {
        guard !completed else { return }
        completed = true
        let cb = callback
        dismiss {
            cb?.onFailed()
        }
    }

    private func finishUnavailable() {
        guard !completed else { return }
        completed = true
        let cb = callback
        dismiss {
            cb?.onUnavailable()
        }
    }

    private func dismiss(done: @escaping () -> Void) {
        callback = nil
        webView?.configuration.userContentController.removeScriptMessageHandler(forName: "rzp")
        webView = nil
        let controller = webController
        webController = nil
        if let controller {
            controller.dismiss(animated: true, completion: done)
        } else {
            done()
        }
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
