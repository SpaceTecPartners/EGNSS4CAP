import UIKit

class SECentroidSampleCell: UITableViewCell, SECell {
    let optionType: SEOptionType = .centroidSample
    
    @IBOutlet weak var countLabel: UILabel!
    @IBOutlet weak var slider: UISlider!
    
    func _reuseImplement(option: SEOption) {
        setCount(count: SEStorage.centroidCount)
    }
    
    @IBAction func sliderChanged(_ sender: Any) {
        setCount(count: Int(round(slider.value)))
    }

    @IBAction func clickDefault(_ sender: Any) {
        setCount(count: SEStorage.defaults[.centroidCount] as! Int)
    }
    
    private func setCount(count: Int) {
        slider.value = Float(count)
        countLabel.text = String(count)
        SEStorage.centroidCount = count
    }
    
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
