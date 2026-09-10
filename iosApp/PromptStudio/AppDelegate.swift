import UIKit
import PromptStudio

// Native lifecycle only; the entire product UI is shared Compose.
@main
final class AppDelegate: UIResponder, UIApplicationDelegate {}

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = MainViewControllerKt.MainViewController()
        window.makeKeyAndVisible()
        self.window = window
    }
}
