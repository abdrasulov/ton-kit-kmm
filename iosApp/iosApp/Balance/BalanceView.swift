import SwiftUI

struct BalanceView: View {
    @StateObject private var viewModel = BalanceViewModel()

    var body: some View {
        VStack(spacing: 16) {
            row(title: "Address", value: viewModel.address)
            row(title: "Balance", value: viewModel.balance.description)
            row(title: "Balance Sync State", value: viewModel.balanceSyncState)
            row(title: "Tx Sync State", value: viewModel.txSyncState)

            Spacer()
        }
        .padding()
        .navigationTitle("Balance")
    }

    @ViewBuilder private func row(title: String, value: String) -> some View {
        HStack(alignment: .top) {
            Text("\(title):")
                .font(.subheadline)

            Spacer()

            Text(value)
                .font(.footnote)
                .multilineTextAlignment(.trailing)
        }
    }
}
