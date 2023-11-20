import SwiftUI

struct MainView: View {
    var body: some View {
        TabView {
            NavigationView {
                BalanceView()
            }
            .tabItem {
                Label("Balance", systemImage: "dollarsign.circle")
            }

            NavigationView {
                TransactionsView()
            }
            .tabItem {
                Label("Transactions", systemImage: "list.bullet")
            }

            Text("Send")
                .tabItem {
                    Label("Send", systemImage: "arrow.up.right.circle")
                }
        }
    }
}
