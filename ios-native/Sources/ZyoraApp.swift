import SwiftUI
import FirebaseCore

@main
struct ZyoraApp: App {
    init() {
        FirebaseApp.configure()
    }

    var body: some Scene {
        WindowGroup {
            ZyoraRootView()
                .preferredColorScheme(.dark)
        }
    }
}

private enum ZyoraStyle {
    static let background = Color(red: 0.035, green: 0.043, blue: 0.075)
    static let surface = Color(red: 0.075, green: 0.086, blue: 0.13)
    static let accent = Color(red: 0.55, green: 0.36, blue: 0.98)
    static let muted = Color.white.opacity(0.58)
}

private struct ZyoraRootView: View {
    @State private var selectedTab = 0
    @State private var email = ""
    @State private var password = ""
    @State private var showingSignIn = false

    var body: some View {
        TabView(selection: $selectedTab) {
            home.tag(0).tabItem { Label("Home", systemImage: "house.fill") }
            rooms.tag(1).tabItem { Label("Rooms", systemImage: "waveform") }
            messages.tag(2).tabItem { Label("Messages", systemImage: "bubble.left.and.bubble.right.fill") }
            profile.tag(3).tabItem { Label("Me", systemImage: "person.crop.circle.fill") }
        }
        .tint(ZyoraStyle.accent)
        .background(ZyoraStyle.background)
    }

    private var home: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    header
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Find your people.")
                            .font(.system(size: 30, weight: .bold, design: .rounded))
                        Text("Talk, listen and make new connections.")
                            .foregroundStyle(ZyoraStyle.muted)
                    }
                    HStack(spacing: 12) {
                        quickCard("Voice rooms", detail: "Join live conversations", icon: "waveform", tab: 1)
                        quickCard("Messages", detail: "Catch up with friends", icon: "message.fill", tab: 2)
                    }
                    sectionTitle("Discover rooms")
                    emptyCard(icon: "headphones", title: "Rooms will appear here", detail: "Connect the Zyora service to load live rooms.")
                    sectionTitle("Your community")
                    emptyCard(icon: "person.2.fill", title: "Make new connections", detail: "Your friends and recommendations will show here.")
                }
                .padding(20)
            }
            .background(ZyoraStyle.background.ignoresSafeArea())
            .toolbar(.hidden, for: .navigationBar)
        }
    }

    private var rooms: some View {
        NavigationStack {
            VStack(spacing: 18) {
                pageHeader("Voice Rooms", subtitle: "Listen in or join the conversation.")
                emptyCard(icon: "waveform", title: "No live rooms loaded", detail: "Live room discovery and Socket.IO connection need the Zyora server endpoint.")
                Spacer()
            }
            .padding(20)
            .background(ZyoraStyle.background.ignoresSafeArea())
        }
    }

    private var messages: some View {
        NavigationStack {
            VStack(spacing: 18) {
                pageHeader("Messages", subtitle: "Your conversations in one place.")
                emptyCard(icon: "bubble.left.and.bubble.right", title: "No conversations yet", detail: "Your synced conversations will appear here after messaging is connected.")
                Spacer()
            }
            .padding(20)
            .background(ZyoraStyle.background.ignoresSafeArea())
        }
    }

    private var profile: some View {
        NavigationStack {
            VStack(spacing: 18) {
                pageHeader("My Profile", subtitle: "Your Zyora identity.")
                Image(systemName: "person.crop.circle.fill")
                    .font(.system(size: 76))
                    .foregroundStyle(ZyoraStyle.accent)
                    .padding(.top, 16)
                Text("Welcome to Zyora")
                    .font(.title2.bold())
                Text("Sign in to access your profile, wallet and settings.")
                    .font(.subheadline)
                    .foregroundStyle(ZyoraStyle.muted)
                    .multilineTextAlignment(.center)
                Button("Sign in") { showingSignIn = true }
                    .buttonStyle(PrimaryButtonStyle())
                Spacer()
            }
            .padding(24)
            .background(ZyoraStyle.background.ignoresSafeArea())
            .sheet(isPresented: $showingSignIn) { signInSheet }
        }
    }

    private var signInSheet: some View {
        NavigationStack {
            VStack(spacing: 16) {
                Text("Welcome to Zyora")
                    .font(.largeTitle.bold())
                Text("Sign in with your Zyora account")
                    .foregroundStyle(ZyoraStyle.muted)
                TextField("Email address", text: $email)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .textFieldStyle(ZyoraFieldStyle())
                SecureField("Password", text: $password)
                    .textFieldStyle(ZyoraFieldStyle())
                Button("Continue") {
                    // Authentication is intentionally not faked. Wire Firebase Auth
                    // after the iOS Firebase app configuration is supplied.
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(email.isEmpty || password.isEmpty)
                Text("Secure sign-in will be enabled when the iOS Firebase configuration is added.")
                    .font(.footnote)
                    .foregroundStyle(ZyoraStyle.muted)
                    .multilineTextAlignment(.center)
                Spacer()
            }
            .padding(24)
            .background(ZyoraStyle.background.ignoresSafeArea())
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Close") { showingSignIn = false } } }
        }
        .presentationDetents([.medium, .large])
    }

    private var header: some View {
        HStack {
            HStack(spacing: 9) {
                Image(systemName: "waveform.circle.fill")
                    .font(.system(size: 30))
                    .foregroundStyle(ZyoraStyle.accent)
                Text("Zyora").font(.title2.bold())
            }
            Spacer()
            Image(systemName: "bell")
                .font(.title3)
                .padding(11)
                .background(ZyoraStyle.surface, in: Circle())
        }
    }

    private func pageHeader(_ title: String, subtitle: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.largeTitle.bold())
            Text(subtitle).foregroundStyle(ZyoraStyle.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.bottom, 8)
    }

    private func sectionTitle(_ title: String) -> some View {
        Text(title).font(.title3.bold())
    }

    private func quickCard(_ title: String, detail: String, icon: String, tab: Int) -> some View {
        Button { selectedTab = tab } label: {
            VStack(alignment: .leading, spacing: 10) {
                Image(systemName: icon).font(.title2).foregroundStyle(ZyoraStyle.accent)
                Text(title).font(.headline).foregroundStyle(.white)
                Text(detail).font(.caption).foregroundStyle(ZyoraStyle.muted)
                    .multilineTextAlignment(.leading)
            }
            .frame(maxWidth: .infinity, minHeight: 112, alignment: .leading)
            .padding(15)
            .background(ZyoraStyle.surface, in: RoundedRectangle(cornerRadius: 18))
        }
        .buttonStyle(.plain)
    }

    private func emptyCard(icon: String, title: String, detail: String) -> some View {
        VStack(spacing: 12) {
            Image(systemName: icon).font(.system(size: 30)).foregroundStyle(ZyoraStyle.accent)
            Text(title).font(.headline)
            Text(detail)
                .font(.subheadline)
                .foregroundStyle(ZyoraStyle.muted)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
        .background(ZyoraStyle.surface, in: RoundedRectangle(cornerRadius: 20))
    }
}

private struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .padding(.vertical, 15)
            .background(ZyoraStyle.accent.opacity(configuration.isPressed ? 0.75 : 1), in: RoundedRectangle(cornerRadius: 14))
    }
}

private struct ZyoraFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(14)
            .background(ZyoraStyle.surface, in: RoundedRectangle(cornerRadius: 12))
            .foregroundStyle(.white)
    }
}
