//
//  AboutViewController.swift
//  GTPhotos
//
//  Created by FoxCom on 22.12.2020.
//

import UIKit

class AboutViewController: UIViewController {

    @IBOutlet weak var textView: UITextView!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        // Support Dark Mode
        if #available(iOS 13.0, *) {
            let attributes = NSMutableAttributedString(attributedString: textView.attributedText!)
            attributes.addAttribute(.foregroundColor, value: UIColor.label, range: NSRange(location: 0, length: attributes.mutableString.length))
            textView.attributedText = attributes
        }
    }
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
