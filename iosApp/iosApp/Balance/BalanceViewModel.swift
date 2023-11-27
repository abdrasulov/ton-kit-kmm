import Combine
import Foundation
import TonKitKmm

class BalanceViewModel: ObservableObject {
    private let tonKit = Singleton.instance.tonKit
    private var cancellables = Set<AnyCancellable>()

    let address: String
    @Published private(set) var balance: Decimal
    @Published private(set) var balanceSyncState: String
    @Published private(set) var txSyncState: String

    init() {
        address = tonKit.receiveAddress
        balance = Singleton.amount(kitAmount: tonKit.balance)
        balanceSyncState = Singleton.syncState(kitSyncState: tonKit.balanceSyncState)
        txSyncState = Singleton.syncState(kitSyncState: tonKit.transactionsSyncState)

        collect(tonKit.balancePublisher)
            .completeOnFailure()
            .sink { [weak self] balance in
                self?.balance = Singleton.amount(kitAmount: balance)
            }
            .store(in: &cancellables)

        collect(tonKit.balanceSyncStatePublisher)
            .completeOnFailure()
            .sink { [weak self] syncState in
                self?.balanceSyncState = Singleton.syncState(kitSyncState: syncState)
            }
            .store(in: &cancellables)

        collect(tonKit.transactionsSyncStatePublisher)
            .completeOnFailure()
            .sink { [weak self] syncState in
                self?.txSyncState = Singleton.syncState(kitSyncState: syncState)
            }
            .store(in: &cancellables)
    }
}
