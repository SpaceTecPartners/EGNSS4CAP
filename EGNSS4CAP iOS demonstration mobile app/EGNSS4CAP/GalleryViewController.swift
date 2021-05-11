//
//  GalleryViewController.swift
//  GTPhotos
//
//  Created by FoxCom on 26.03.2021.
//

import UIKit

class GalleryViewController: UIViewController {

    @IBOutlet weak var idLabel: UILabel!
    @IBOutlet weak var orderLabel: UILabel!
    @IBOutlet weak var latLabel: UILabel!
    @IBOutlet weak var longLabel: UILabel!
    @IBOutlet weak var createdLabel: UILabel!
    @IBOutlet weak var photoImage: UIImageView!
    @IBOutlet weak var counterLabel: UILabel!
    
    var persistPhotos = [PersistPhoto]()
    var currentPhotoIndex = 0
    var taskid:Int64 = -1
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        loadPhoto()

        // Do any additional setup after loading the view.
    }
    
    override func viewWillAppear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.all)
    }   
    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */
    @IBAction func leftButton(_ sender: UIButton) {
        if currentPhotoIndex > 0 {
            currentPhotoIndex = currentPhotoIndex - 1
            loadPhoto()
        }
    }
    
    @IBAction func rightButton(_ sender: UIButton) {
        if currentPhotoIndex < (persistPhotos.count - 1){
            currentPhotoIndex = currentPhotoIndex + 1
            loadPhoto()
        }
    }
    
    func loadPhoto() {
        idLabel.text = String(taskid)
        orderLabel.text = String(currentPhotoIndex + 1)
        latLabel.text = persistPhotos[currentPhotoIndex].lat.description
        longLabel.text = persistPhotos[currentPhotoIndex].lng.description
        
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        createdLabel.text = df.string(from: persistPhotos[currentPhotoIndex].created!)
        
        photoImage.image = UIImage(data: persistPhotos[currentPhotoIndex].photo!)
        
        counterLabel.text = String(currentPhotoIndex+1) + "/" + String(persistPhotos.count)
    }

}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
