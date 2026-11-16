import Testing
import UnicodeIdent

@Suite struct UnicodeIdentExportTests {
    @Test func asciiLettersStartAndContinueIdentifiers() {
        for ch in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".utf16 {
            #expect(isXidStart(ch: ch))
            #expect(isXidContinue(ch: ch))
        }
    }

    @Test func digitsAndUnderscoreOnlyContinueIdentifiers() {
        for ch in "0123456789_".utf16 {
            #expect(!isXidStart(ch: ch))
            #expect(isXidContinue(ch: ch))
        }
        for ch in " !@#$%^&*()-+=".utf16 {
            #expect(!isXidStart(ch: ch))
            #expect(!isXidContinue(ch: ch))
        }
    }

    @Test func nonAsciiLettersStartIdentifiers() {
        for ch in "α中é".utf16 {
            #expect(isXidStart(ch: ch))
            #expect(isXidContinue(ch: ch))
        }
    }
}
