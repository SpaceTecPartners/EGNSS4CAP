import Foundation
import UIKit

enum SESection: Int, CaseIterable{
    case centroid
    
    var title: String {
        switch self {
        case .centroid:
            return "Centroid"
        }
    }
    
    var optionCount: Int {
        var count = 0
        for opt in SEOption.allCases {
            if opt.section == self {
                count += 1
            }
        }
        return count
    }
}

enum SEOptionType: Int, CaseIterable {
    case active
    case centroidSample
    
    var reuseIdentifier: String {
        switch self {
        case .active:
            return "activeRI"
        case .centroidSample:
            return "centroidSampleRI"
        }
    }
}

enum SEOption: Int, CaseIterable {
    case centroidActive
    case centoidSample
    
    var section: SESection {
        switch self {
        case .centroidActive:
            return .centroid
        case .centoidSample:
            return .centroid
        }
    }
    
    var type: SEOptionType {
        switch self {
        case .centroidActive:
            return .active
        case .centoidSample:
            return .centroidSample
        }
    }
}

protocol SECell: UITableViewCell  {
    
    var optionType: SEOptionType {get}
    
    func _reuseImplement(option: SEOption)
}
extension SECell{
    func reuse(option: SEOption) {
        self.selectionStyle = .none
        _reuseImplement(option: option)
    }
}
