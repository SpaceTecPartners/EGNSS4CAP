//
//  TasksTableViewController.swift
//  GTPhotos
//
//  Created by FoxCom on 24.03.2021.
//

import UIKit
import CoreData
import CryptoKit

class TasksTableViewController: UITableViewController {
    
    var persistTasks = [PersistTask]()
    var manageObjectContext: NSManagedObjectContext!

    override func viewDidLoad() {
        super.viewDidLoad()
        
        manageObjectContext = (UIApplication.shared.delegate as! AppDelegate).persistentContainer.viewContext

        getNewTasks()
        
        // Uncomment the following line to preserve selection between presentations
        // self.clearsSelectionOnViewWillAppear = false

        // Uncomment the following line to display an Edit button in the navigation bar for this view controller.
        // self.navigationItem.rightBarButtonItem = self.editButtonItem
    }
    
    override func viewWillAppear(_ animated:Bool) {
       AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
        
        loadPersistTasks()
        tableView.reloadData()        
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.all)
    }

    // MARK: - Table view data source

    override func numberOfSections(in tableView: UITableView) -> Int {
        // #warning Incomplete implementation, return the number of sections
        return 1
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        // #warning Incomplete implementation, return the number of rows
        return persistTasks.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        
        let cellIdentifier = "TaskTableViewCell"
        
        guard let cell = tableView.dequeueReusableCell(withIdentifier: cellIdentifier, for: indexPath) as? TasksTableViewCell  else {
            fatalError("The dequeued cell is not an instance of MealTableViewCell.")
        }
        
        let task = persistTasks[indexPath.row]
            
        cell.nameLabel.text = task.name
        cell.statusLabel.text = task.status
        
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        cell.createdLabel.text = df.string(from: task.date_created!)
        cell.dueLabel.text = df.string(from: task.task_due_date!)
        if (task.task_due_date! < Date()) {
            cell.dueLabel.textColor = UIColor.systemRed;
        } else {
            cell.dueLabel.textColor = UIColor.systemGreen
        }
        
        if task.status == "new" {
            cell.statusImage.image = UIImage(named: "status_new")
        } else if task.status == "open"{
            cell.statusImage.image = UIImage(named: "status_open")
        } else if task.status == "returned"{
            cell.statusImage.image = UIImage(named: "status_returned")
        } else {
            cell.statusImage.image = UIImage(named: "status_provided")
        }
        
        var persistPhotos = [PersistPhoto]()
        
        let persistPhotoRequest: NSFetchRequest<PersistPhoto> = PersistPhoto.fetchRequest()
        persistPhotoRequest.predicate = NSPredicate(format: "userid == %@ AND taskid == %i", String(UserStorage.userID), task.id)
        do {
            persistPhotos = try manageObjectContext.fetch(persistPhotoRequest)
        }
        catch {
            print("Could not load save data: \(error.localizedDescription)")
        }
          
        cell.countLabel.text = String(persistPhotos.count) + " photos"
        if cell.countLabel.text == "1 photos" {
            cell.countLabel.text = "1 photo"
        }
        
       
               
        return cell
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        
        super.prepare(for: segue, sender: sender)
        
        switch(segue.identifier ?? "") {
            
        case "ShowTaskDetail":
            guard let taskViewController = segue.destination as? TaskViewController else {
                fatalError("Unexpected destination: \(segue.destination)")
            }
            
            guard let selectedTaskCell = sender as? TasksTableViewCell else {
                fatalError("Unexpected sender: \(String(describing: sender))")
            }
            
            guard let indexPath = tableView.indexPath(for: selectedTaskCell) else {
                fatalError("The selected cell is not being displayed by the table")
            }
            
            let selectedTask = persistTasks[indexPath.row]
            taskViewController.persistTask = selectedTask
            taskViewController.manageObjectContext = manageObjectContext
            
        default:
            fatalError("Unexpected Segue Identifier; \(String(describing: segue.identifier))")
        }
    }
      
    
    /*
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "reuseIdentifier", for: indexPath)

        // Configure the cell...

        return cell
    }
    */

    /*
    // Override to support conditional editing of the table view.
    override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool {
        // Return false if you do not want the specified item to be editable.
        return true
    }
    */

    /*
    // Override to support editing the table view.
    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCell.EditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            // Delete the row from the data source
            tableView.deleteRows(at: [indexPath], with: .fade)
        } else if editingStyle == .insert {
            // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
        }    
    }
    */

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
    
    private func loadPersistTasks() {
        let persistTaskRequest: NSFetchRequest<PersistTask> = PersistTask.fetchRequest()
        let sortDescriptor = NSSortDescriptor(key: "date_created", ascending: false)
        let sortDescriptors = [sortDescriptor]
        persistTaskRequest.sortDescriptors = sortDescriptors
        persistTaskRequest.predicate = NSPredicate(format: "userid == %@", String(UserStorage.userID))
        do {
            persistTasks = try manageObjectContext.fetch(persistTaskRequest)
        }
        catch {
            print("Could not load save data: \(error.localizedDescription)")
        }
    }
    
    func getNewTasks() {
        let waitAlert = UIAlertController(title: nil, message: "Loading, please wait...", preferredStyle: .alert)
        let loadingIndicator = UIActivityIndicatorView(frame: CGRect(x: 0, y: 5, width: 50, height: 50))
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.style = .medium
        loadingIndicator.startAnimating();
        waitAlert.view.addSubview(loadingIndicator)
        
        self.present(waitAlert, animated: true, completion: nil)
        
        let userID = String(UserStorage.userID)
        
        do {
            // Prepare URL
            let url = URL(string: "https://login:pswd@egnss4cap-uat.foxcom.eu/ws/comm_tasks.php")
            guard let requestUrl = url else { fatalError() }
            // Prepare URL Request Object
            var request = URLRequest(url: requestUrl)
            request.httpMethod = "POST"
             
            // HTTP Request Parameters which will be sent in HTTP Request Body
            let postString = "user_id="+userID
            // Set HTTP Request Body
            request.httpBody = postString.data(using: String.Encoding.utf8);
            // Perform HTTP Request
            let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
                // Check for Error
                if error != nil {
                    DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
                        waitAlert.dismiss(animated: true) {
                            print("Loading tasks error 1.")
                        }
                    }
                    return
                }
                // Convert HTTP Response Data to a String
                if let data = data, let dataString = String(data: data, encoding: .utf8) {
                    DispatchQueue.main.asyncAfter(deadline: DispatchTime.now() + 0.5) {
                        waitAlert.dismiss(animated: true) {
                            print("Tasks loaded from server.")
                            self.processResponseData(data: dataString)
                        }
                    }
                }
            }
            task.resume()
            
        } catch {
            print(error)
        }
        
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
            struct Task: Decodable {
                var id: String
                var status: String
                var name: String?
                var text: String?
                var text_returned: String?
                var date_created: String
                var task_due_date: String
                var note: String?
            }
            struct Answer: Decodable {
                var status: String
                var error_msg: String?
                var tasks: [Task]?
            }
            
            let userID = String(UserStorage.userID)
            let df = DateFormatter()
            df.dateFormat = "yyyy-MM-dd HH:mm:ss"
            
            let jsonData = data.data(using: .utf8)!
            let answer = try! JSONDecoder().decode(Answer.self, from: jsonData)
            
            if answer.status == "ok" {
                for task in answer.tasks ?? [] {
                    if task.status == "new" {
                                
                        let persistTaskRequest: NSFetchRequest<PersistTask> = PersistTask.fetchRequest()
                        persistTaskRequest.predicate = NSPredicate(format: "userid == %@ and id == %@", userID, task.id)
                        
                        var perTasks = [PersistTask]()
                        do {
                            perTasks = try manageObjectContext.fetch(persistTaskRequest)
                            
                            if perTasks.count == 0 {
                                print("inserting task")
                                let persistTask = PersistTask(context: manageObjectContext)
                                
                                persistTask.userid = Int64(userID) ?? 0
                                persistTask.id = Int64(task.id) ?? 0
                                persistTask.status = task.status
                                persistTask.name = task.name
                                persistTask.text = task.text
                                persistTask.text_returned = task.text_returned
                                persistTask.date_created = df.date(from: task.date_created)
                                persistTask.task_due_date = df.date(from: task.task_due_date)
                                
                                do{
                                    try self.manageObjectContext.save()
                                }catch{
                                    print("Could not save data: \(error.localizedDescription)")
                                }
                            }
                        }
                        catch {
                            print("Could not load save data: \(error.localizedDescription)")
                        }                       
                    }
                    if task.status == "returned" {
                        let persistTaskRequest: NSFetchRequest<PersistTask> = PersistTask.fetchRequest()
                        persistTaskRequest.predicate = NSPredicate(format: "userid == %@ and id == %@ and status <> %@", userID, task.id, "returned")
                        
                        var perTasks = [PersistTask]()
                        do {
                            perTasks = try manageObjectContext.fetch(persistTaskRequest)
                            
                            if perTasks.count > 0 {
                                print("updating task")
                                let persistTask = perTasks[0]
                                
                                persistTask.text_returned = task.text_returned
                                persistTask.status = task.status
                                
                                do{
                                    try self.manageObjectContext.save()
                                }catch{
                                    print("Could not save data: \(error.localizedDescription)")
                                }
                            }
                        }
                        catch {
                            print("Could not load save data: \(error.localizedDescription)")
                        }
                    }
                }
            
            }
            
        }
        
        loadPersistTasks()
        tableView.reloadData()
    }

}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
