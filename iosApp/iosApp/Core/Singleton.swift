import Combine
import Foundation
import TonKitKmm

struct Singleton {
    static let instance = Singleton()

    private let watchAddress = "UQBpAeJL-VSLCigCsrgGQHCLeiEBdAuZBlbrrUGI4BVQJoPM"

    let tonKit: TonKit

    init() {
        tonKit = TonKitFactory(driverFactory: DriverFactory()).createWatch(address: watchAddress, walletId: "watch")
        tonKit.start()
    }
}
