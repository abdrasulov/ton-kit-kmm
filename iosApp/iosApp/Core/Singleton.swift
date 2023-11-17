import Combine
import Foundation
import shared

struct Singleton {
    static let instance = Singleton()

    private let watchAddress = "UQBpAeJL-VSLCigCsrgGQHCLeiEBdAuZBlbrrUGI4BVQJoPM"

    let tonKit: TonKit

    init() {
        tonKit = TonKitFactory(driverFactory: DriverFactory()).createWatch(address: watchAddress)
        tonKit.start()
    }
}
