import Foundation

struct MyanmarPhoneInfo: Sendable {
    let valid: Bool
    let normalized: String
    let operatorName: String
    let network: String
}

enum MyanmarUtilities {
    static func checkPhone(_ raw: String) -> MyanmarPhoneInfo {
        let digitsMap: [Character: Character] = [
            "၀": "0", "၁": "1", "၂": "2", "၃": "3", "၄": "4",
            "၅": "5", "၆": "6", "၇": "7", "၈": "8", "၉": "9"
        ]
        var ascii = String(raw.map { digitsMap[$0] ?? $0 })
            .replacingOccurrences(of: " ", with: "")
            .replacingOccurrences(of: "-", with: "")
            .replacingOccurrences(of: "(", with: "")
            .replacingOccurrences(of: ")", with: "")

        if ascii.hasPrefix("+959") { ascii = "09" + ascii.dropFirst(4) }
        else if ascii.hasPrefix("959") { ascii = "09" + ascii.dropFirst(3) }
        else if ascii.hasPrefix("+95") { ascii = "0" + ascii.dropFirst(3) }

        let valid = ascii.range(of: #"^09\d{7,9}$"#, options: .regularExpression) != nil
        guard valid else {
            return MyanmarPhoneInfo(valid: false, normalized: ascii, operatorName: "Unknown", network: "Invalid format")
        }

        if ascii.range(of: #"^09(9[4-8])\d{7}$"#, options: .regularExpression) != nil {
            return MyanmarPhoneInfo(valid: true, normalized: ascii, operatorName: "Ooredoo", network: "GSM / 4G")
        }
        if ascii.range(of: #"^09(7[4-9])\d{7}$"#, options: .regularExpression) != nil {
            return MyanmarPhoneInfo(valid: true, normalized: ascii, operatorName: "ATOM (Telenor)", network: "GSM / 4G")
        }
        if ascii.range(of: #"^09(6[5-9])\d{7}$"#, options: .regularExpression) != nil {
            return MyanmarPhoneInfo(valid: true, normalized: ascii, operatorName: "Mytel", network: "GSM / 4G")
        }
        if ascii.range(of: #"^09(3[0-6])\d{6,7}$"#, options: .regularExpression) != nil {
            return MyanmarPhoneInfo(valid: true, normalized: ascii, operatorName: "MEC", network: "CDMA / GSM")
        }
        return MyanmarPhoneInfo(valid: true, normalized: ascii, operatorName: "MPT", network: "GSM / 4G")
    }

    static func myanmarDateSummary(for date: Date) -> (year: Int, month: String, weekday: String) {
        let cal = Calendar.current
        let y = cal.component(.year, from: date)
        let m = cal.component(.month, from: date)
        let d = cal.component(.day, from: date)
        let wd = cal.component(.weekday, from: date) // 1 = Sun

        let beforeThingyan = m < 4 || (m == 4 && d < 17)
        let meYear = y - (beforeThingyan ? 639 : 638)

        let months = [
            "တန်ခူး (Tagu)", "ကဆုန် (Kason)", "နယုန် (Nayon)", "ဝါဆို (Waso)",
            "ဝါခေါင် (Wagaung)", "တော်သလင်း (Tawthalin)", "သီတင်းကျွတ် (Thadingyut)",
            "တန်ဆောင်မုန်း (Tazaungmon)", "နတ်တော် (Nadaw)", "ပြာသို (Pyatho)",
            "တပို့တွဲ (Tabodwe)", "တပေါင်း (Tabaung)"
        ]
        let weekdays = [
            "တနင်္ဂနွေ (Sunday)", "တနင်္လာ (Monday)", "အင်္ဂါ (Tuesday)",
            "ဗုဒ္ဓဟူး (Wednesday)", "ကြာသပတေး (Thursday)", "သောကြာ (Friday)", "စနေ (Saturday)"
        ]
        return (meYear, months[(m + 8) % 12], weekdays[max(0, min(6, wd - 1))])
    }
}
