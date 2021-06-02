//
//  PhotoDetailViewController.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 05/11/2020.
//

import UIKit
import CoreData

class PhotoDetailViewController: UIViewController {
    
    var persistPhoto: PersistPhoto!
    var manageObjectContext: NSManagedObjectContext!

    @IBOutlet weak var photoImageView: UIImageView!
    @IBOutlet weak var sendButton: UIBarButtonItem!
    @IBOutlet weak var metaView: UIView!
    @IBOutlet weak var latValueLabel: UILabel!
    @IBOutlet weak var lngValueLabel: UILabel!
    @IBOutlet weak var createdValueLabel: UILabel!
    @IBOutlet weak var sendedValueLabel: UILabel!
    @IBOutlet weak var noteValueLabel: UILabel!
    @IBOutlet weak var noteButton: UIBarButtonItem!
    
    @IBAction func send(_ sender: UIBarButtonItem) {
        let waitAlert = UIAlertController(title: nil, message: "Sending, please wait...", preferredStyle: .alert)
        let loadingIndicator = UIActivityIndicatorView(frame: CGRect(x: 0, y: 5, width: 50, height: 50))
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.style = .medium
        loadingIndicator.startAnimating();
        waitAlert.view.addSubview(loadingIndicator)
        
        self.present(waitAlert, animated: true, completion: nil)
        
        let userID = String(UserStorage.userID)
        
        struct Photo:Codable {
            var lat:Double
            var lng:Double
            var altitude:Double
            var bearing:Double
            var magnetic_azimuth:Double
            var photo_heading:Double
            var accuracy:Double
            var orientation:Int64
            var pitch:Double
            var roll:Double
            var photo_angle:Double
            var created:String
            var note:String
            var photo:String
            var digest:String
        }
        
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        let stringDate = df.string(from: persistPhoto.created!)
        
        let data:Data = persistPhoto.photo!
        let base64String:String = data.base64EncodedString(options: NSData.Base64EncodingOptions(rawValue:0))
        
        let photo = Photo(lat:persistPhoto.lat, lng:persistPhoto.lng, altitude: persistPhoto.altitude, bearing: persistPhoto.bearing, magnetic_azimuth: persistPhoto.azimuth, photo_heading: persistPhoto.photoHeading, accuracy: persistPhoto.accuracy, orientation: persistPhoto.orientation, pitch: persistPhoto.pitch, roll: persistPhoto.roll, photo_angle: persistPhoto.tilt, created: stringDate, note: persistPhoto.note ?? "", photo: base64String, digest:persistPhoto.digest!)
        
        do {
            let jsonData = try JSONEncoder().encode(photo)
            let jsonString = String(data: jsonData, encoding: .utf8)!
            
            // Prepare URL
            let url = URL(string: "https://login:password@egnss4cap-uat.foxcom.eu/ws/comm_photo.php")
            guard let requestUrl = url else { fatalError() }
            // Prepare URL Request Object
            var request = URLRequest(url: requestUrl)
            request.httpMethod = "POST"
             
            // HTTP Request Parameters which will be sent in HTTP Request Body
            let postString = "user_id="+userID+"&photo="+jsonString
            // Set HTTP Request Body
            request.httpBody = postString.data(using: String.Encoding.utf8);
            // Perform HTTP Request
            let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
                // Check for Error
                if error != nil {
                    DispatchQueue.main.async {
                        waitAlert.dismiss(animated: true) {
                            self.showConnError()
                        }
                    }
                    return
                }
                // Convert HTTP Response Data to a String
                if let data = data, let dataString = String(data: data, encoding: .utf8) {
                    DispatchQueue.main.async {
                        waitAlert.dismiss(animated: true) {
                            self.processResponseData(data: dataString)
                        }
                    }
                }
            }
            task.resume()
            
        } catch { print(error) }
    }
    
    @IBAction func editNote(_ sender: UIBarButtonItem) {
        //1. Create the alert controller.
        let alert = UIAlertController(title: "Photo note", message: "", preferredStyle: .alert)

        //2. Add the text field. You can configure it however you need.
        alert.addTextField { (textField) in
            textField.text = self.persistPhoto.note
        }

        // 3. Grab the value from the text field, and print it when the user clicks OK.
        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [weak alert] (_) in
            let textField = alert?.textFields![0] // Force unwrapping because we know it exists.
            
            self.persistPhoto.note = textField?.text
            
            do{
                try self.manageObjectContext.save()
            }catch{
                print("Could not save data: \(error.localizedDescription)")
            }
            
            self.updateDetail()
        }))

        // 4. Present the alert.
        self.present(alert, animated: true, completion: nil)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        metaView.layer.cornerRadius = 10
        
        updateDetail()        
        updateSendBUtton()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.all)
    }
    
    func updateSendBUtton() {
        if persistPhoto.sended == true {
            sendButton.isEnabled = false
            noteButton.isEnabled = false
        } else {
            sendButton.isEnabled = true
            noteButton.isEnabled = true
        }
    }
    
    func updateDetail() {
        photoImageView.image = UIImage(data: persistPhoto.photo!)
        latValueLabel.text = persistPhoto.lat.description
        lngValueLabel.text = persistPhoto.lng.description
        /* DEBUGCOM
        latValueLabel.text = persistPhoto.centroidLat.description
        lngValueLabel.text = persistPhoto.centroidLng.description
        /**/*/
        
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        createdValueLabel.text = df.string(from: persistPhoto.created!)
        
        if persistPhoto.sended == true {
            sendedValueLabel.text = "yes"
        } else {
            sendedValueLabel.text = "no"
        }
        
        noteValueLabel.text = persistPhoto.note
    }
    
    func showConnError() {
        let alert = UIAlertController(title: "Sending error", message: "Connection error", preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "Ok", style: UIAlertAction.Style.default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    }
    
    func showSendingError() {
        let alert = UIAlertController(title: "Sending error", message: "Could not send photo", preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "Ok", style: UIAlertAction.Style.default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    }
    
    func showSendingSuccess() {
        let alert = UIAlertController(title: "Sending succesfull", message: "Photo was succesfully sent.", preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "Ok", style: UIAlertAction.Style.default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    }
    
    func processResponseData(data:String) {
        //print("Response data string:\n \(data)")
        struct Answer: Decodable {
            var status: String
            var error_msg: String?
        }

        let jsonData = data.data(using: .utf8)!
        let answer = try! JSONDecoder().decode(Answer.self, from: jsonData)
        
        if answer.status == "ok" {
            showSendingSuccess()
            
            persistPhoto.sended = true
            
            do{
                try self.manageObjectContext.save()
            }catch{
                print("Could not save data: \(error.localizedDescription)")
            }
            
            updateDetail()
            updateSendBUtton()
        } else {
            showSendingError()
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
