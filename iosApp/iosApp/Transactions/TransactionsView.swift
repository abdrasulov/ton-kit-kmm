import SwiftUI

struct TransactionsView: View {
    @StateObject private var viewModel = TransactionsViewModel()

    var body: some View {
        List(viewModel.transactions, id: \.hash) { transaction in
            VStack(spacing: 4) {
                row(title: "Hash", value: transaction.hash)
                row(title: "Type", value: transaction.type)
                row(title: "Fee", value: transaction.fee?.description ?? "n/a")
                row(title: "Timestamp", value: String(transaction.timestamp))
                row(title: "Lt", value: String(transaction.lt))

                if !transaction.transfers.isEmpty {
                    Text("Transfers:".uppercased()).font(.footnote).frame(maxWidth: .infinity, alignment: .leading)

                    VStack(spacing: 16) {
                        ForEach(transaction.transfers.indices, id: \.self) { index in
                            let transfer = transaction.transfers[index]

                            VStack(spacing: 4) {
                                row(title: "From", value: transfer.src)
                                row(title: "To", value: transfer.dest)
                                row(title: "Value", value: transfer.amount.description)
                            }
                        }
                    }
                    .padding(.top, 8)
                    .padding(.leading, 32)
                }
            }
        }
        .navigationTitle("Transactions")
    }

    @ViewBuilder private func row(title: String, value: String) -> some View {
        HStack(spacing: 12) {
            Text("\(title.uppercased()):")
                .font(.footnote)

            Spacer()

            Text(value)
                .font(.caption2)
                .multilineTextAlignment(.trailing)
                .lineLimit(1)
                .truncationMode(.middle)
        }
    }
}
