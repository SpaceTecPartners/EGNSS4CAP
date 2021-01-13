//
//  ViewController.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 03/11/2020.
//

import UIKit
import CoreLocation

class MainViewController: UIViewController {

    @IBOutlet weak var userView: UIView!
    @IBOutlet weak var userTitleView: UIView!
    @IBOutlet weak var basicInfoView: UIView!
    @IBOutlet weak var basicInfoTitleView: UIView!
    @IBOutlet weak var loginLabel: UILabel!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var surnameLabel: UILabel!
    @IBOutlet weak var locationCheckImage: UIImageView!
    @IBOutlet weak var galileoCheckImage: UIImageView!
    @IBOutlet weak var logoView: UIView!
    @IBOutlet weak var buttonView: UIView!
    @IBOutlet weak var serviceView: UIView!
    @IBOutlet weak var galileoView: UIView!
    
    let locationManager = CLLocationManager()
    private var timer: Timer!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        userView.layer.cornerRadius = 10
        userTitleView.layer.cornerRadius = 10
        userTitleView.layer.maskedCorners = [.layerMaxXMinYCorner, .layerMinXMinYCorner]
        userView.layer.shadowColor = UIColor.black.cgColor
        userView.layer.shadowOffset = CGSize(width: 3, height: 3)
        userView.layer.shadowOpacity = 0.3
        userView.layer.shadowRadius = 2.0
        
        basicInfoView.layer.cornerRadius = 10
        basicInfoTitleView.layer.cornerRadius = 10
        basicInfoTitleView.layer.maskedCorners = [.layerMaxXMinYCorner, .layerMinXMinYCorner]
        basicInfoView.layer.shadowColor = UIColor.black.cgColor
        basicInfoView.layer.shadowOffset = CGSize(width: 3, height: 3)
        basicInfoView.layer.shadowOpacity = 0.3
        basicInfoView.layer.shadowRadius = 2.0
        
        logoView.layer.cornerRadius = 10
        logoView.layer.shadowColor = UIColor.black.cgColor
        logoView.layer.shadowOffset = CGSize(width: 3, height: 3)
        logoView.layer.shadowOpacity = 0.3
        logoView.layer.shadowRadius = 2.0       
        
        buttonView.layer.cornerRadius = 10
        buttonView.layer.shadowColor = UIColor.black.cgColor
        buttonView.layer.shadowOffset = CGSize(width: 3, height: 3)
        buttonView.layer.shadowOpacity = 0.3
        buttonView.layer.shadowRadius = 2.0
        
        locationManager.requestWhenInUseAuthorization()
        
        updateLoggedUser()
        updateBasicInfo()
        
        timer = Timer.scheduledTimer(timeInterval: 10, target: self, selector: #selector(updateBasicInfo), userInfo: nil, repeats: true)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.all)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        checkLoggedUser()
    }
    
    @IBAction func unwindToMainView(sender: UIStoryboardSegue) {
        updateLoggedUser()        
    }

    @IBAction func photosButton(_ sender: UIButton) {
        performSegue(withIdentifier: "ShowPhotos", sender: self)
    }
    
    @IBAction func mapButton(_ sender: UIButton) {
        performSegue(withIdentifier: "ShowMap", sender: self)
    }
    
    @IBAction func settingsButton(_ sender: UIButton) {
        performSegue(withIdentifier: "ShowSettings", sender: self)
    }
    @IBAction func aboutButton(_ sender: UIButton) {
        performSegue(withIdentifier: "ShowAbout", sender: self)
    }
        
    @IBAction func logout(_ sender: UIBarButtonItem) {
        UserStorage.removeObject(key: UserStorage.Key.userID)
        UserStorage.removeObject(key: UserStorage.Key.login)
        UserStorage.removeObject(key: UserStorage.Key.userName)
        UserStorage.removeObject(key: UserStorage.Key.userSurname)
        
        updateLoggedUser()
        
        checkLoggedUser()
    }    
    
    private func checkLoggedUser() {
        let isLogged = UserStorage.exists(key: UserStorage.Key.userID)
        
        if isLogged != true {
            performSegue(withIdentifier: "ShowLoginScreen", sender: self)
        }
    }
    
    func updateLoggedUser() {
        loginLabel.text = UserStorage.login
        nameLabel.text = UserStorage.userName
        surnameLabel.text = UserStorage.userSurname
    }
    
    @objc func updateBasicInfo() {
        if CLLocationManager.locationServicesEnabled() {
            switch CLLocationManager.authorizationStatus() {
                case .notDetermined, .restricted, .denied:
                    print("No access")
                    locationCheckImage.image = UIImage(named: "red_circle")
                case .authorizedAlways, .authorizedWhenInUse:
                    print("Access")
                    locationCheckImage.image = UIImage(named: "green_circle")
                @unknown default:
                break
            }
        } else {
            print("Location services are not enabled")
            locationCheckImage.image = UIImage(named: "red_circle")
        }
        
        if (UIDevice().model == "iPhone") {
            switch UIDevice().type {
                case .iPhone4, .iPhone4S, .iPhone5, .iPhone5C, .iPhone5S, .iPhone6Plus, .iPhone6, .iPhone6S, .iPhone6SPlus, .iPhoneSE, .iPhone7, .iPhone7Plus: galileoCheckImage.image = UIImage(named: "red_circle")
                default: galileoCheckImage.image = UIImage(named: "green_circle")
            }
        } else {
            galileoCheckImage.image = UIImage(named: "red_circle")
        }       
        
    }
    
}

