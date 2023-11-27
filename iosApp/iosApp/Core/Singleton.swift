import Combine
import Foundation
import TonKitKmm

struct Singleton {
    static let instance = Singleton()
    private static let coinRate: Decimal = 1_000_000_000

    private let watchAddress = "UQDd5wJZ_lA98nktDHFqVfXIU9j3ZNtDt_8Zm3kB530jBWMZ"

    let tonKit: TonKit

    init() {
        tonKit = TonKitFactory(driverFactory: DriverFactory(), connectionManager: ConnectionManager()).createWatch(address: watchAddress, walletId: "watch")
        tonKit.start()
    }

    static func amount(kitAmount: String) -> Decimal {
        let rawKitValue: Decimal = Decimal(string: kitAmount) ?? 0
        return rawKitValue / Self.coinRate
    }

    static func syncState(kitSyncState: AnyObject) -> String {
        switch kitSyncState {
        case is TonKitKmm.SyncState.Syncing: return "Syncing"
        case is TonKitKmm.SyncState.Synced: return "Synced"
        case is TonKitKmm.SyncState.NotSynced: return "Not Synced"
        default: return "n/a"
        }
    }
}
