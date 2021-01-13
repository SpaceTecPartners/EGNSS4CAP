import UIKit

class SEActiveCell: UITableViewCell, SECell {
    
    private class Holder {
        typealias LoadType = () -> Bool
        typealias SaveType = (_ on: Bool) -> Void
        
        let label: String
        let load: LoadType
        let save: SaveType
        
        init(label: String, load: @escaping LoadType, save: @escaping SaveType) {
            self.label = label
            self.load = load
            self.save = save
        }
    }
    
    let optionType = SEOptionType.active
    
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var activeSwitch: UISwitch!
            
    private var holder: Holder!
    
    func _reuseImplement(option: SEOption) {
        activeSwitch.addTarget(self, action: #selector(valueChanged), for: .valueChanged)
        switch option {
        case .centroidActive:
            holder = Holder(label: "Photo with centroid location computation", load: {
                return SEStorage.centroidActive
            }, save: {active in
                SEStorage.centroidActive = active
            })
        default:
            break
        }
        reuseInit()
    }
    
    private func reuseInit() {
        titleLabel.text = holder.label
        activeSwitch.setOn(holder!.load(), animated: false)
        
    }
    
    @objc private func valueChanged() {
        holder.save(activeSwitch.isOn)
    }
    
}
