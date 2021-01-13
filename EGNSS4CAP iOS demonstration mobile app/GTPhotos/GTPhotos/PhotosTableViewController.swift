//
//  PhotosTableViewController.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 04/11/2020.
//

import UIKit
import CoreData
import CryptoKit

extension Digest {
    var bytes: [UInt8] { Array(makeIterator()) }
    var data: Data { Data(bytes) }

    var hexStr: String {
        bytes.map { String(format: "%02X", $0) }.joined()
    }
}

class PhotosTableViewController: UITableViewController {
    
    var persistPhotos = [PersistPhoto]()
    var manageObjectContext: NSManagedObjectContext!
    
    var openDetail = false
    var scrollDown = false

    @IBOutlet weak var editButton: UIBarButtonItem!
    @IBOutlet weak var newButton: UIBarButtonItem!
    
    @IBAction func edit(_ sender: UIBarButtonItem) {
        if(tableView.isEditing == true)
            {
                tableView.isEditing = false
                editButton.title = "Edit"
                newButton.isEnabled = true
            }
            else
            {
                tableView.isEditing = true
                editButton.title = "Done"
                newButton.isEnabled = false
            }
    }   
    
    @IBAction func newPhoto(_ sender: UIBarButtonItem) {
        performSegue(withIdentifier: "ShowCamera", sender: self)
    }
    
    @IBAction func unwindToTableView(sender: UIStoryboardSegue) {
        loadPersistPhotos()
        openDetail = true
    }
    
    func scrollToBottom(){
            DispatchQueue.main.async {
                let indexPath = IndexPath(row: self.persistPhotos.count-1, section: 0)
                self.tableView.scrollToRow(at: indexPath, at: .top, animated: false)
            }
        }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        tableView.tableFooterView = UIView()
        
        manageObjectContext = (UIApplication.shared.delegate as! AppDelegate).persistentContainer.viewContext
        
        loadPersistPhotos()        
        /*
        if persistPhotos.count == 0 {
            loadSamplePhotos()
        }*/
    }
    
    override func viewWillAppear(_ animated:Bool) {
       AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
        
        if openDetail == true {
            openDetail = false
            scrollDown = true
            performSegue(withIdentifier: "ShowPhotoDetailManual", sender: self)
        } else {
            tableView.reloadData()
            
            if scrollDown == true {
                scrollDown = false
                scrollToBottom()
            }
        }
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.all)
    }

    // MARK: - Table view data source

    override func numberOfSections(in tableView: UITableView) -> Int {
        
        return 1
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        
        return persistPhotos.count
    }

    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cellIdentifier = "PhotoTableViewCell"
        
        guard let cell = tableView.dequeueReusableCell(withIdentifier: cellIdentifier, for: indexPath) as? PhotoTableViewCell  else {
            fatalError("The dequeued cell is not an instance of MealTableViewCell.")
        }
        
        let photo = persistPhotos[indexPath.row]
            
        cell.latValueLabel.text = photo.lat.description
        cell.lngValueLabel.text = photo.lng.description
        
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        cell.createdValueLabel.text = df.string(from: photo.created!)
        
        if photo.sended == true {
            cell.sendedValueLabel.text = "yes"
        } else {
            cell.sendedValueLabel.text = "no"
        }
        
        cell.noteValueLabel.text = photo.note
        
        cell.photoImage.image = UIImage(data: photo.photo!)
        
        return cell
    }
        

    
    // Override to support conditional editing of the table view.
    override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool {
        // Return false if you do not want the specified item to be editable.
        return true
    }
    

    
    // Override to support editing the table view.
    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCell.EditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            // Delete the row from the data source
            
            manageObjectContext.delete(persistPhotos[indexPath.row] as NSManagedObject)
            persistPhotos.remove(at: indexPath.row)
            tableView.deleteRows(at: [indexPath], with: .fade)
            
            do{
                try self.manageObjectContext.save()
            }catch{
                print("Could not save data: \(error.localizedDescription)")
            }
            
        } else if editingStyle == .insert {
            // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
        }    
    }
    

    /*
    // Override to support rearranging the table view.
    override func tableView(_ tableView: UITableView, moveRowAt fromIndexPath: IndexPath, to: IndexPath) {

    }
    */

    /*
    // Override to support conditional rearranging of the table view.
    override func tableView(_ tableView: UITableView, canMoveRowAt indexPath: IndexPath) -> Bool {
        // Return false if you do not want the item to be re-orderable.
        return true
    }
    */

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        
        super.prepare(for: segue, sender: sender)
        
        switch(segue.identifier ?? "") {
        
        case "ShowCamera":
            guard let cameraViewController = segue.destination as? CameraViewController else {
                fatalError("Unexpected destination: \(segue.destination)")
            }
            
            cameraViewController.persistPhotos = persistPhotos
            cameraViewController.manageObjectContext = manageObjectContext
            
        case "ShowPhotoDetail":
            guard let photoDetailViewController = segue.destination as? PhotoDetailViewController else {
                fatalError("Unexpected destination: \(segue.destination)")
            }
            
            guard let selectedPhotoCell = sender as? PhotoTableViewCell else {
                fatalError("Unexpected sender: \(String(describing: sender))")
            }
            
            guard let indexPath = tableView.indexPath(for: selectedPhotoCell) else {
                fatalError("The selected cell is not being displayed by the table")
            }
            
            let selectedPhoto = persistPhotos[indexPath.row]
            photoDetailViewController.persistPhoto = selectedPhoto
            photoDetailViewController.manageObjectContext = manageObjectContext
        
        case "ShowPhotoDetailManual":
            guard let photoDetailViewController = segue.destination as? PhotoDetailViewController else {
                fatalError("Unexpected destination: \(segue.destination)")
            }            
            
            let selectedPhoto = persistPhotos[self.persistPhotos.count-1]
            photoDetailViewController.persistPhoto = selectedPhoto
            photoDetailViewController.manageObjectContext = manageObjectContext
            
        default:
            fatalError("Unexpected Segue Identifier; \(String(describing: segue.identifier))")
        }
    }
    
    private func loadPersistPhotos() {
        let persistPhotoRequest: NSFetchRequest<PersistPhoto> = PersistPhoto.fetchRequest()
        persistPhotoRequest.predicate = NSPredicate(format: "userid == %@", String(UserStorage.userID))
        do {
            persistPhotos = try manageObjectContext.fetch(persistPhotoRequest)
        }
        catch {
            print("Could not load save data: \(error.localizedDescription)")
        }
    }
    
    private func loadSamplePhotos() {
        let img1 = UIImage(named: "tree")
        let img2 = UIImage(named: "tree")
        
        let userID = String(UserStorage.userID)
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        
        let persistPhoto1 = PersistPhoto(context: manageObjectContext)
        
        persistPhoto1.userid = Int64(userID) ?? 0
        persistPhoto1.lat = 1.0
        persistPhoto1.lng = 1.1
        persistPhoto1.created = Date()
        persistPhoto1.sended = false
        persistPhoto1.note = "pozn1"
        persistPhoto1.photo = img1?.jpegData(compressionQuality: 1)
        
        let stringDate1 = df.string(from: persistPhoto1.created!)
        let photo_hash_string1 = SHA256.hash(data: persistPhoto1.photo!).hexStr.lowercased()
        let digest_string1 = "bfb576892e43b763731a1596c428987893b2e76ce1be10f733_" + photo_hash_string1 + "_" + stringDate1 + "_" + userID
        persistPhoto1.digest = SHA256.hash(data: digest_string1.data(using: .utf8)!).hexStr.lowercased()
        
        let persistPhoto2 = PersistPhoto(context: manageObjectContext)
        
        persistPhoto2.userid = Int64(userID) ?? 0
        persistPhoto2.lat = 2.0
        persistPhoto2.lng = 2.1
        persistPhoto2.created = Date()
        persistPhoto2.sended = false
        persistPhoto2.note = "pozn2"
        persistPhoto2.photo = img2?.jpegData(compressionQuality: 1)
        
        let stringDate2 = df.string(from: persistPhoto2.created!)
        let photo_hash_string2 = SHA256.hash(data: persistPhoto2.photo!).hexStr.lowercased()
        let digest_string2 = "bfb576892e43b763731a1596c428987893b2e76ce1be10f733_" + photo_hash_string2 + "_" + stringDate2 + "_" + userID
        persistPhoto2.digest = SHA256.hash(data: digest_string2.data(using: .utf8)!).hexStr.lowercased()
        
        persistPhotos += [persistPhoto1, persistPhoto2]
               
        do{
            try self.manageObjectContext.save()
        }catch{
            print("Could not save data: \(error.localizedDescription)")
        }
        
        print("Sample data loaded.")
    }
    
    
    
    
        
    
}
