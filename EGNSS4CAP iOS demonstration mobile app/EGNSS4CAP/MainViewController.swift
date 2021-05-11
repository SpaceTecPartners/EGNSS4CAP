//
//  ViewController.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 03/11/2020.
//

import UIKit
import CoreLocation
import CoreData

class MainViewController: UIViewController {
    
    var manageObjectContext: NSManagedObjectContext!

    @IBOutlet weak var userView: UIView!
    @IBOutlet weak var basicInfoView: UIView!
    @IBOutlet weak var loginLabel: UILabel!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var surnameLabel: UILabel!
    @IBOutlet weak var locationCheckImage: UIImageView!
    @IBOutlet weak var galileoCheckImage: UIImageView!
    @IBOutlet weak var buttonView: UIView!
    @IBOutlet weak var serviceView: UIView!
    @IBOutlet weak var galileoView: UIView!
    
    let locationManager = CLLocationManager()
    private var timer: Timer!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view.
        
        manageObjectContext = (UIApplication.shared.delegate as! AppDelegate).persistentContainer.viewContext
        
        userView.layer.cornerRadius = 10
        /*userTitleView.layer.cornerRadius = 10
        userTitleView.layer.maskedCorners = [.layerMaxXMinYCorner, .layerMinXMinYCorner]
        userView.layer.shadowColor = UIColor.black.cgColor
        userView.layer.shadowOffset = CGSize(width: 3, height: 3)
        userView.layer.shadowOpacity = 0.3
        userView.layer.shadowRadius = 2.0*/
        
        basicInfoView.layer.cornerRadius = 10
        /*basicInfoTitleView.layer.cornerRadius = 10
        basicInfoTitleView.layer.maskedCorners = [.layerMaxXMinYCorner, .layerMinXMinYCorner]
        basicInfoView.layer.shadowColor = UIColor.black.cgColor
        basicInfoView.layer.shadowOffset = CGSize(width: 3, height: 3)
        basicInfoView.layer.shadowOpacity = 0.3
        basicInfoView.layer.shadowRadius = 2.0*/
        
        /*buttonView.layer.cornerRadius = 10
        buttonView.layer.shadowColor = UIColor.black.cgColor
        buttonView.layer.shadowOffset = CGSize(width: 3, height: 3)
        buttonView.layer.shadowOpacity = 0.3
        buttonView.layer.shadowRadius = 2.0*/
        
        locationManager.requestWhenInUseAuthorization()
        
        //updateLoggedUser()
        //updateBasicInfo()
        
        timer = Timer.scheduledTimer(timeInterval: 10, target: self, selector: #selector(updateBasicInfo), userInfo: nil, repeats: true)
    }
    
    override func viewWillAppear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
        
        updateLoggedUser()
        updateBasicInfo()
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
    
    @IBAction func tasksButton(_ sender: UIButton) {
        performSegue(withIdentifier: "ShowTasks", sender: self)
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
        
        //updateLoggedUser()
        
        checkLoggedUser()
    }    
    
    private func checkLoggedUser() {
        let isLogged = UserStorage.exists(key: UserStorage.Key.userID)
        
        if isLogged != true {
            performSegue(withIdentifier: "ShowLoginScreen", sender: self)
        }
    }
    
    func updateLoggedUser() {
        if UserStorage.exists(key: UserStorage.Key.userID) == true {
            loginLabel.text = UserStorage.login
            nameLabel.text = getOpenTasksCount().description
            surnameLabel.text = getPhotoCount().description
        } else {
            loginLabel.text = ""
            nameLabel.text = ""
            surnameLabel.text = ""
        }        
    }
    
    private func getOpenTasksCount() -> Int {
        var tasks = [PersistTask]()
        
        let persistTasksRequest: NSFetchRequest<PersistTask> = PersistTask.fetchRequest()
        persistTasksRequest.predicate = NSPredicate(format: "userid == %@ AND status = 'open'", String(UserStorage.userID))
        do {
            tasks = try manageObjectContext.fetch(persistTasksRequest)
        }
        catch {
            print("Could not load save data: \(error.localizedDescription)")
        }
        
        return tasks.count
    }
    
    private func getPhotoCount() -> Int {
        var persistPhotos = [PersistPhoto]()
        
        let persistPhotoRequest: NSFetchRequest<PersistPhoto> = PersistPhoto.fetchRequest()
        persistPhotoRequest.predicate = NSPredicate(format: "userid == %@", String(UserStorage.userID))
        do {
            persistPhotos = try manageObjectContext.fetch(persistPhotoRequest)
        }
        catch {
            print("Could not load save data: \(error.localizedDescription)")
        }
        
        return persistPhotos.count
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
        
        if (UserStorage.exists(key: UserStorage.Key.gpsCapable) != true) {
            var capableType: Bool
            if (UIDevice().model == "iPhone") {
                switch UIDevice().type {
                    case .iPhone4, .iPhone4S, .iPhone5, .iPhone5C, .iPhone5S, .iPhone6Plus, .iPhone6: capableType = false
                    default: capableType = true
                }
            } else if (UIDevice().model == "iPad") {
                switch UIDevice().type {
                    case .iPad2, .iPad3, .iPad4, .iPadMini, .iPadMini2, .iPadMini3, .iPadMini4, .iPadAir, .iPadAir2: capableType = false
                    default: capableType = true
                }
            } else {
                capableType = false
            }
            
            var positiveAltitude: Bool
            if (self.locationManager.location?.altitude ?? 0 > 0) {
                positiveAltitude = true
            } else {
                positiveAltitude = false
            }
            
            if (capableType && positiveAltitude) {
                UserStorage.gpsCapable = true
            }
        }
        
        if (UserStorage.exists(key: UserStorage.Key.gpsCapable) == true) {
            galileoCheckImage.image = UIImage(named: "green_circle")
        } else {
            galileoCheckImage.image = UIImage(named: "red_circle")
        }       
        
    }
    
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
