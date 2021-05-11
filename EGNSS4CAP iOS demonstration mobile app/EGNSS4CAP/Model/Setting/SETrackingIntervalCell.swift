import UIKit

class SETrackingIntervalCell: UITableViewCell, SECell {
    let optionType: SEOptionType = .interval
    
    @IBOutlet weak var countLabel: UILabel!
    @IBOutlet weak var slider: UISlider!
    
    
    func _reuseImplement(option: SEOption) {
        setCount(count: SEStorage.trackingInterval)
    }
    
   
    @IBAction func sliderChanged(_ sender: Any) {
        setCount(count: Int(round(slider.value)))
    }
    
    @IBAction func clickDefault(_ sender: Any) {
        setCount(count: SEStorage.defaults[.trackingInterval] as! Int)
    }
    
    
    private func setCount(count: Int) {
        slider.value = Float(count)
        countLabel.text = String(count)
        SEStorage.trackingInterval = count
    }
    
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
