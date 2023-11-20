import SwiftUI

struct TransactionsView: View {
    @StateObject private var viewModel = TransactionsViewModel()

    var body: some View {
        List(viewModel.transactions, id: \.hash) { transaction in
            VStack(spacing: 8) {
                row(title: "Hash", value: transaction.hash)
                row(title: "Type", value: transaction.type)
                row(title: "Value", value: transaction.value ?? "n/a")
                row(title: "From", value: transaction.src ?? "n/a")
                row(title: "To", value: transaction.dest ?? "n/a")
                row(title: "Timestamp", value: String(transaction.timestamp))
                row(title: "Lt", value: String(transaction.lt))
            }
        }
        .navigationTitle("Transactions")
    }

    @ViewBuilder private func row(title: String, value: String) -> some View {
        HStack(alignment: .top) {
            Text("\(title):")
                .font(.footnote)

            Spacer()

            Text(value)
                .font(.caption2)
                .multilineTextAlignment(.trailing)
        }
    }
}
