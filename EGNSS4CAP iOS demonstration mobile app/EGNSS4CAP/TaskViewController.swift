//
//  TaskViewController.swift
//  GTPhotos
//
//  Created by FoxCom on 25.03.2021.
//

import UIKit
import CoreData

class TaskViewController: UIViewController {
    
    var persistTask : PersistTask!
    var persistPhotos = [PersistPhoto]()
    var manageObjectContext: NSManagedObjectContext!
    
    var currentPhotoIndex = 0
    
    var waitAlert:UIAlertController!
    var photosToSend = [PersistPhoto]()

    @IBOutlet weak var idLabel: UILabel!
    @IBOutlet weak var nameLabel: UILabel!
    @IBOutlet weak var statusLabel: UILabel!
    @IBOutlet weak var createdLabel: UILabel!
    @IBOutlet weak var dueLabel: UILabel!
    @IBOutlet weak var textView: UITextView!
    @IBOutlet weak var returnLabel: UILabel!
    @IBOutlet weak var noteLabel: UILabel!
    @IBOutlet weak var noteButton: UIButton!
    @IBOutlet weak var cameraButton: UIButton!
    @IBOutlet weak var deleteButton: UIButton!
    @IBOutlet weak var photoImage: UIImageView!
    @IBOutlet weak var latLabel: UILabel!
    @IBOutlet weak var longLabel: UILabel!
    @IBOutlet weak var photoCreatedLabel: UILabel!
    @IBOutlet weak var orderLabel: UILabel!
    @IBOutlet weak var sendButton: UIButton!
    @IBOutlet weak var galleryButton: UIButton!
    @IBOutlet weak var returnView: UIView!
    @IBOutlet weak var noteCons1: NSLayoutConstraint!
    @IBOutlet weak var noteCons2: NSLayoutConstraint!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        changeStatus()
        
        loadPersistPhotos()
        
        updateDetail()
        updateSendButton()
        loadPhoto()

        // Do any additional setup after loading the view.
    }
    
    override func viewWillAppear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.all)
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        
        super.prepare(for: segue, sender: sender)
        
        switch(segue.identifier ?? "") {
            
        case "ShowTaskCamera":
            guard let cameraViewController = segue.destination as? CameraViewController else {
                fatalError("Unexpected destination: \(segue.destination)")
            }
            
            cameraViewController.taskid = persistTask.id
            //cameraViewController.persistPhotos = persistPhotos
            cameraViewController.manageObjectContext = manageObjectContext
            
        case "ShowTaskPhotoGallery":
            print("gallery")
            guard let galleryViewController = segue.destination as? GalleryViewController else {
                fatalError("Unexpected destination: \(segue.destination)")
            }
            
            galleryViewController.taskid = persistTask.id
            galleryViewController.currentPhotoIndex = currentPhotoIndex
            galleryViewController.persistPhotos = persistPhotos
            
        default:
            fatalError("Unexpected Segue Identifier; \(String(describing: segue.identifier))")
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
    @IBAction func noteButton(_ sender: UIButton) {
        //1. Create the alert controller.
        let alert = UIAlertController(title: "Task note", message: "", preferredStyle: .alert)

        //2. Add the text field. You can configure it however you need.
        alert.addTextField { (textField) in
            textField.text = self.persistTask.note
        }

        // 3. Grab the value from the text field, and print it when the user clicks OK.
        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: { [weak alert] (_) in
            let textField = alert?.textFields![0] // Force unwrapping because we know it exists.
            
            self.persistTask.note = textField?.text
            
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
    
    @IBAction func deleteButton(_ sender: UIButton) {
        let msg = "Delete current photo?"
        let confirmAlert = UIAlertController(title: "Confirm deletion", message: msg, preferredStyle: .alert)
        confirmAlert.addAction(UIAlertAction(title: "OK", style: .default, handler: deletePhoto))
        confirmAlert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
        self.present(confirmAlert, animated: true, completion: nil)
    }
    
    @IBAction func cameraButton(_ sender: UIButton) {
        performSegue(withIdentifier: "ShowTaskCamera", sender: self)
    }
    
    @IBAction func prevButton(_ sender: Any) {
        if currentPhotoIndex > 0 {
            currentPhotoIndex = currentPhotoIndex - 1
            loadPhoto()
        }
    }
    
    @IBAction func nextButton(_ sender: Any) {
        if currentPhotoIndex < (persistPhotos.count - 1){
            currentPhotoIndex = currentPhotoIndex + 1
            loadPhoto()
        }
    }
    
    @IBAction func sendButton(_ sender: UIButton) {
        let msg = "Submit this task including all its photos? It cannot be changed afterwards."
        let confirmAlert = UIAlertController(title: "Confirm submission", message: msg, preferredStyle: .alert)
        confirmAlert.addAction(UIAlertAction(title: "OK", style: .default, handler: sendTask))
        confirmAlert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
        self.present(confirmAlert, animated: true, completion: nil)
    }
    
    @IBAction func galleryButton(_ sender: UIButton) {
        performSegue(withIdentifier: "ShowTaskPhotoGallery", sender: self)
    }
    
    func changeStatus() {
        
        if persistTask.status == "new" {
            persistTask.status = "open"
            
            do{
                try self.manageObjectContext.save()
            }catch{
                print("Could not save data: \(error.localizedDescription)")
            }
        }
    }
    
    @IBAction func unwindToTaskView(sender: UIStoryboardSegue) {
        loadPersistPhotos()
        currentPhotoIndex = persistPhotos.count - 1
        loadPhoto()
        updateSendButton()
    }
    
    func deletePhoto(alert: UIAlertAction!) {
        manageObjectContext.delete(persistPhotos[currentPhotoIndex] as NSManagedObject)
        persistPhotos.remove(at: currentPhotoIndex)
        
        if persistPhotos.count == 0 {
            currentPhotoIndex = 0
        } else {
            if currentPhotoIndex > (persistPhotos.count - 1) {
                currentPhotoIndex = persistPhotos.count - 1
            }
        }
        loadPhoto()
        updateSendButton()
                
        do{
            try self.manageObjectContext.save()
        }catch{
            print("Could not save data: \(error.localizedDescription)")
        }
    }
    
    private func loadPersistPhotos() {
        let persistPhotoRequest: NSFetchRequest<PersistPhoto> = PersistPhoto.fetchRequest()
        persistPhotoRequest.predicate = NSPredicate(format: "userid == %@ AND taskid == %i", String(UserStorage.userID), persistTask.id)
        do {
            persistPhotos = try manageObjectContext.fetch(persistPhotoRequest)
        }
        catch {
            print("Could not load save data: \(error.localizedDescription)")
        }
    }
    
    func updateDetail() {
        
        if persistTask.status == "data provided" {
            noteButton.isHidden = true;
            cameraButton.isHidden = true;
            deleteButton.isHidden = true;
        } else {
            noteButton.isHidden = false;
            cameraButton.isHidden = false;
            deleteButton.isHidden = false;
        }
        
        if persistTask.status == "returned" {
            returnView.isHidden = false
            noteCons1.isActive = true
            noteCons2.isActive = false
        } else {
            returnView.isHidden = true
            noteCons1.isActive = false
            noteCons2.isActive = true
        }
        
        idLabel.text = String(persistTask.id)
        nameLabel.text = persistTask.name
        statusLabel.text = persistTask.status
        
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        createdLabel.text = df.string(from: persistTask.date_created!)
        dueLabel.text = df.string(from: persistTask.task_due_date!)
        textView.text = persistTask.text
        returnLabel.text = persistTask.text_returned
        noteLabel.text = persistTask.note
    }
    
    func updateSendButton() {
        if persistTask.status != "data provided" {
            let persistPhotoRequest: NSFetchRequest<PersistPhoto> = PersistPhoto.fetchRequest()
            persistPhotoRequest.predicate = NSPredicate(format: "userid == %@ AND taskid == %i AND sended == false", String(UserStorage.userID), persistTask.id)
            do {
                photosToSend = try manageObjectContext.fetch(persistPhotoRequest)
            }
            catch {
                print("Could not load save data: \(error.localizedDescription)")
            }
            
            if photosToSend.count > 0 {
                sendButton.isEnabled = true
            } else {
                sendButton.isEnabled = false
            }
        } else {
            sendButton.isEnabled = false
        }
    }
    
    func loadPhoto() {
        if (persistPhotos.count > 0) {
            orderLabel.text = String(currentPhotoIndex+1) + "/" + String(persistPhotos.count)
            
            photoImage.image = UIImage(data: persistPhotos[currentPhotoIndex].photo!)
            latLabel.text = persistPhotos[currentPhotoIndex].lat.description
            longLabel.text = persistPhotos[currentPhotoIndex].lng.description
            
            let df = DateFormatter()
            df.dateFormat = "yyyy-MM-dd HH:mm:ss"
            photoCreatedLabel.text = df.string(from: persistPhotos[currentPhotoIndex].created!)
            
            if persistPhotos[currentPhotoIndex].sended == false {
                deleteButton.isEnabled = true
            } else {
                deleteButton.isEnabled = false
            }
            
            galleryButton.isEnabled = true;
        } else {
            orderLabel.text = "0/0"
            photoImage.image = UIImage(named: "empty_image")
            latLabel.text = ""
            longLabel.text = ""
            photoCreatedLabel.text = ""
            
            deleteButton.isEnabled = false
            
            galleryButton.isEnabled = false;
        }
    }
    
    func sendTask(alert: UIAlertAction!) {
        waitAlert = UIAlertController(title: nil, message: "Sending, please wait...", preferredStyle: .alert)
        let loadingIndicator = UIActivityIndicatorView(frame: CGRect(x: 0, y: 5, width: 50, height: 50))
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.style = .medium
        loadingIndicator.startAnimating();
        waitAlert.view.addSubview(loadingIndicator)
        
        self.present(waitAlert, animated: true, completion: nil)
        
        sendPhoto()
    }
    
    func sendPhoto() {
        let persistPhotoRequest: NSFetchRequest<PersistPhoto> = PersistPhoto.fetchRequest()
        persistPhotoRequest.predicate = NSPredicate(format: "userid == %@ AND taskid == %i AND sended == false", String(UserStorage.userID), persistTask.id)
        do {
            photosToSend = try manageObjectContext.fetch(persistPhotoRequest)
        }
        catch {
            print("Could not load save data: \(error.localizedDescription)")
        }
        
        if (photosToSend.count > 0) {
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
            let stringDate = df.string(from: photosToSend[0].created!)
            
            let data:Data = photosToSend[0].photo!
            let base64String:String = data.base64EncodedString(options: NSData.Base64EncodingOptions(rawValue:0))
            
            let photo = Photo(lat:photosToSend[0].lat, lng:photosToSend[0].lng, altitude: photosToSend[0].altitude, bearing: photosToSend[0].bearing, magnetic_azimuth: photosToSend[0].azimuth, photo_heading: photosToSend[0].photoHeading, accuracy: photosToSend[0].accuracy, orientation: photosToSend[0].orientation, pitch: photosToSend[0].pitch, roll: photosToSend[0].roll, photo_angle: photosToSend[0].tilt, created: stringDate, note: photosToSend[0].note ?? "", photo: base64String, digest:photosToSend[0].digest!)
            
            do {
                let jsonData = try JSONEncoder().encode(photo)
                let jsonString = String(data: jsonData, encoding: .utf8)!
                
                // Prepare URL
                let url = URL(string: "https://login:pswd@egnss4cap-uat.foxcom.eu/ws/comm_photo.php")
                guard let requestUrl = url else { fatalError() }
                // Prepare URL Request Object
                var request = URLRequest(url: requestUrl)
                request.httpMethod = "POST"
                 
                // HTTP Request Parameters which will be sent in HTTP Request Body
                let postString = "user_id="+userID+"&task_id="+String(persistTask.id)+"&photo="+jsonString
                // Set HTTP Request Body
                request.httpBody = postString.data(using: String.Encoding.utf8);
                // Perform HTTP Request
                let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
                    // Check for Error
                    if error != nil {
                        DispatchQueue.main.async {
                            self.waitAlert.dismiss(animated: true) {
                                self.showConnError()
                            }
                        }
                        return
                    }
                    // Convert HTTP Response Data to a String
                    if let data = data, let dataString = String(data: data, encoding: .utf8) {
                        DispatchQueue.main.async {
                            self.processResponseData1(data: dataString)
                        }
                    }
                }
                task.resume()
                
            } catch { print(error) }
        } else {
            sendStatus()
        }
    }
    
    func processResponseData1(data:String) {
        //print("Response data string:\n \(data)")
        struct Answer: Decodable {
            var status: String
            var error_msg: String?
        }

        let jsonData = data.data(using: .utf8)!
        let answer = try! JSONDecoder().decode(Answer.self, from: jsonData)
        
        if answer.status == "ok" {
            photosToSend[0].sended = true
            
            do{
                try self.manageObjectContext.save()
            }catch{
                print("Could not save data: \(error.localizedDescription)")
            }
            
            sendPhoto()
        } else {
            waitAlert.dismiss(animated: true) {
                self.showSendingError()
            }
        }
    }
    
    func sendStatus() {
                
        do {
            let statusString = "data provided"
            let noteString = persistTask.note ?? ""
                        
            // Prepare URL
            let url = URL(string: "https://login:pswd@egnss4cap-uat.foxcom.eu/ws/comm_status.php")
            guard let requestUrl = url else { fatalError() }
            // Prepare URL Request Object
            var request = URLRequest(url: requestUrl)
            request.httpMethod = "POST"
             
            // HTTP Request Parameters which will be sent in HTTP Request Body
            let postString = "task_id="+String(persistTask.id)+"&status="+statusString+"&note="+noteString
            // Set HTTP Request Body
            request.httpBody = postString.data(using: String.Encoding.utf8);
            // Perform HTTP Request
            let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
                // Check for Error
                if error != nil {
                    DispatchQueue.main.async {
                        self.waitAlert.dismiss(animated: true) {
                            self.showConnError()
                        }
                    }
                    return
                }
                // Convert HTTP Response Data to a String
                if let data = data, let dataString = String(data: data, encoding: .utf8) {
                    DispatchQueue.main.async {
                        self.processResponseData2(data: dataString)
                    }
                }
            }
            task.resume()
            
        } catch { print(error) }
    }
    
    func processResponseData2(data:String) {
        //print("Response data string:\n \(data)")
        struct Answer: Decodable {
            var status: String
            var error_msg: String?
        }

        let jsonData = data.data(using: .utf8)!
        let answer = try! JSONDecoder().decode(Answer.self, from: jsonData)
        
        if answer.status == "ok" {
            persistTask.status = "data provided"
            
            do{
                try self.manageObjectContext.save()
            }catch{
                print("Could not save data: \(error.localizedDescription)")
            }
            
            waitAlert.dismiss(animated: true) {
                self.showSendingSuccess()
            }
        } else {
            waitAlert.dismiss(animated: true) {
                self.showSendingError()
            }
        }
    }
    
    func showConnError() {
        let alert = UIAlertController(title: "Sending error", message: "Connection error", preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "Ok", style: UIAlertAction.Style.default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    
        updateDetail()
        updateSendButton()
        loadPhoto()
    }
    
    func showSendingError() {       
        let alert = UIAlertController(title: "Sending error", message: "Could not send task", preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "Ok", style: UIAlertAction.Style.default, handler: nil))
        self.present(alert, animated: true, completion: nil)
        
        updateDetail()
        updateSendButton()
        loadPhoto()
    }
    
    func showSendingSuccess() {
        let alert = UIAlertController(title: "Sending succesfull", message: "Task was succesfully sent.", preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "Ok", style: UIAlertAction.Style.default, handler: nil))
        self.present(alert, animated: true, completion: nil)
        
        updateDetail()
        updateSendButton()
        loadPhoto()
    }
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
