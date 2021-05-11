//
//  PhotoTableViewCell.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 04/11/2020.
//

import UIKit

class PhotoTableViewCell: UITableViewCell {

    @IBOutlet weak var latValueLabel: UILabel!
    @IBOutlet weak var lngValueLabel: UILabel!
    @IBOutlet weak var createdValueLabel: UILabel!
    @IBOutlet weak var sendedValueLabel: UILabel!
    @IBOutlet weak var noteValueLabel: UILabel!
    @IBOutlet weak var photoImage: UIImageView!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }

    override func setSelected(_ selected: Bool, animated: Bool) {
        super.setSelected(selected, animated: animated)

        // Configure the view for the selected state
    }

}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
