import Testing
import UnicodeIdent

@Suite struct UnicodeIdentExportTests {
    @Test func swiftModuleLoads() throws {
        #expect(Bool(true), "UnicodeIdent swift module imported cleanly")
    }
}
