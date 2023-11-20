import Combine
import Foundation
import TonKitKmm

class BalanceViewModel: ObservableObject {
    private let tonKit = Singleton.instance.tonKit
    private var cancellables = Set<AnyCancellable>()

    let address: String
    @Published private(set) var balance: String?
    @Published private(set) var balanceSyncState: String = ""
    @Published private(set) var txSyncState: String = ""

    init() {
        address = tonKit.receiveAddress

        collect(tonKit.balancePublisher)
            .completeOnFailure()
            .sink { [weak self] balance in
                self?.balance = balance
            }
            .store(in: &cancellables)

        collect(tonKit.balanceSyncStatePublisher)
            .completeOnFailure()
            .sink { [weak self] syncState in
                self?.balanceSyncState = Self.syncState(kitSyncState: syncState)
            }
            .store(in: &cancellables)

        collect(tonKit.transactionsSyncStatePublisher)
            .completeOnFailure()
            .sink { [weak self] syncState in
                self?.txSyncState = Self.syncState(kitSyncState: syncState)
            }
            .store(in: &cancellables)
    }

    private static func syncState(kitSyncState: AnyObject) -> String {
        switch kitSyncState {
        case is TonKitKmm.SyncState.Syncing: return "Syncing"
        case is TonKitKmm.SyncState.Synced: return "Synced"
        case is TonKitKmm.SyncState.NotSynced: return "Not Synced"
        default: return "n/a"
        }
    }
}
