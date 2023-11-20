import Combine
import Foundation
import TonKitKmm

class TransactionsViewModel: ObservableObject {
    private let tonKit = Singleton.instance.tonKit
    private var cancellables = Set<AnyCancellable>()

    @Published var transactions = [Transaction]()

    init() {
        collect(tonKit.doNewTransactionsPublisher)
            .completeOnFailure()
            .sink { [weak self] _ in
                self?.fetch()
            }
            .store(in: &cancellables)

        fetch()
    }

    private func fetch() {
        Task { [tonKit] in
            let tonTransactions = try await tonKit.transactions(fromTransactionHash: nil, type: nil, limit: 100)

            DispatchQueue.main.async { [weak self] in
                self?.handle(tonTransactions: tonTransactions)
            }
        }
    }

    private func handle(tonTransactions: [TonTransaction]) {
        let transactions = tonTransactions.map { tx in
            Transaction(
                hash: tx.hash,
                lt: Int(tx.lt),
                timestamp: Int(tx.timestamp),
                value: tx.value_,
                type: tx.type,
                src: tx.src,
                dest: tx.dest
            )
        }

        self.transactions = transactions
    }
}

struct Transaction {
    let hash: String
    let lt: Int
    let timestamp: Int
    let value: String?
    let type: String
    let src: String?
    let dest: String?
}
