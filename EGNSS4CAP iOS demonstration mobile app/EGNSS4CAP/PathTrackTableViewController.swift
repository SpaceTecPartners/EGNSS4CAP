import UIKit
import CoreData

class PathTrackTableViewController: UITableViewController {
    
    let db = DB()
    
    var paths: [PTPath] = []
    var selectedPath: PTPath?
    let sendDQ = DispatchQueue(label: "sendDQ")
    let sendDB = DB()
    let c = DB().privateMOC
    
    @IBOutlet weak var editButton: UIBarButtonItem!
    @IBOutlet weak var sendButton: UIBarButtonItem!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        tableView.tableFooterView = UIView()
        
        loadPaths()
    }
    
    func loadPaths() {
        paths = PTPath.selectByActualUser(manageObjectContext: db.mainMOC)
    }
    
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        
        return paths.count
    }
    
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let path = paths[indexPath.row]
        
        let cell = tableView.dequeueReusableCell(withIdentifier: PathTrackTableViewCell.indentifier, for: indexPath) as! PathTrackTableViewCell
        cell.nameLabel.text = path.name
        cell.startLabel.text = Util.prettyDate(date: path.start!) + " " + Util.prettyTime(date: path.start!)
        cell.endLabel.text = Util.prettyDate(date: path.end!) + " " + Util.prettyTime(date: path.end!)
        cell.areaLabel.text = path.area.description
        cell.sentLabel.text = path.sent ? "Yes" : "No"
        
        return cell
    }
    
    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCell.EditingStyle, forRowAt indexPath: IndexPath) {
        switch editingStyle {
        case .delete:
            db.mainMOC.delete(paths[indexPath.row])
            try! db.mainMOC.save()
            paths.remove(at: indexPath.row)
            tableView.deleteRows(at: [indexPath], with: .top)
            break
        default:
            break
        }
    }
    
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        selectedPath = paths[indexPath.row]
        performSegue(withIdentifier: "ShowPathInMap", sender: self)

    }
    
    @IBAction func editTable(_ sender: UIBarButtonItem) {
        if(tableView.isEditing == true){
            tableView.isEditing = false
            editButton.title = "Edit"
            sendButton.isEnabled = true
        } else {
            tableView.isEditing = true
            editButton.title = "Done"
            sendButton.isEnabled = false
        }
    }
    
    @IBAction func sendAllAction(_ sender: UIBarButtonItem) {
        let pathsToSend = self.pathToSend(context: db.mainMOC)
        
        let toUpload = pathsToSend.count > 0
        var msg = "\(pathsToSend.count) Paths not uploaded."
        if (msg == "1 Paths not uploaded.") {
            msg = "1 Path not uploaded."
        }
        let snedAlert = UIAlertController(title: "Send All", message: msg, preferredStyle: .alert)
        snedAlert.addAction(UIAlertAction(title: "OK", style: .default, handler: toUpload ? sendAll : nil))
        if toUpload {
            snedAlert.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
        }
        self.present(snedAlert, animated: true, completion: nil)
    }
    
    func sendAll(alert: UIAlertAction!) {
        let waitAlert = UIAlertController(title: nil, message: "Sending, please wait...", preferredStyle: .alert)
        let loadingIndicator = UIActivityIndicatorView(frame: CGRect(x: 0, y: 5, width: 50, height: 50))
        loadingIndicator.hidesWhenStopped = true
        loadingIndicator.style = .medium
        loadingIndicator.startAnimating();
        waitAlert.view.addSubview(loadingIndicator)
        sendDQ.async {
            DispatchQueue.main.sync {
                self.present(waitAlert, animated: true, completion: nil)
            }
            
            self.sendDB.privateMOC.reset()
            self.sendDB.privateMOC.retainsRegisteredObjects = true
            let pathsToSend = self.pathToSend(context: self.sendDB.privateMOC)
            var groups = [DispatchGroup]()
            for path in pathsToSend {
                let dispatchGroup = DispatchGroup()
                groups.append(dispatchGroup)
                dispatchGroup.enter()
                if !self.sendPath(ptPath: path, dispatchGroup: dispatchGroup) {
                    dispatchGroup.leave()
                }
            }
            for group in groups {
                group.wait()
            }
            
            DispatchQueue.main.sync {
                waitAlert.dismiss(animated: true) {
                    self.showSendAllResults()
                    self.tableView.reloadData()
                }
            }
        }
    }
    
    func sendPath(ptPath: PTPath, dispatchGroup: DispatchGroup) -> Bool {
        guard let ptPointsSet = ptPath.points else {
            return false
        }
        
        let ptPoints = ptPointsSet.array as! [PTPoint]
        
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm:ss"
        struct RPoint: Codable {
            var lat: Double
            var lng: Double
            var created: String
        }
        var rPoints = [RPoint]()
        for p in ptPoints {
            rPoints.append(RPoint(lat: p.lat, lng: p.lng, created: df.string(from: p.created!)))
        }
        let pointData: JSONEncoder.Output
        do {
            pointData = try JSONEncoder().encode(rPoints)
        } catch {
            print("Error encoding path to JSON: \(error.localizedDescription)" )
            return false
        }
        
        let jsonPointsString = String(data: pointData, encoding: .utf8)!
        let params: [String: String] = [
            "user_id": String(UserStorage.userID),
            "name": ptPath.name!,
            "start": df.string(from: ptPath.start!),
            "end": df.string(from: ptPath.end!),
            "area": ptPath.area.description,
            "points": jsonPointsString
        ]
        
        let url = URL(string: "https://login:pswd@egnss4cap-uat.foxcom.eu/ws/comm_path.php")
        guard let requestUrl = url else { fatalError() }
        var request = URLRequest(url: requestUrl)
        request.httpMethod = "POST"
        let postString = Util.encodeParameters(params: params)
        request.httpBody = postString.data(using: .utf8)
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            if error != nil {
                print("Error while sending path: \(error!.localizedDescription))")
            } else if let data = data, let dataString = String(data: data, encoding: .utf8) {
                self.sendDQ.async {
                    self.processResponse(ptPath: ptPath, data: dataString)
                }
            }
            dispatchGroup.leave()
        }
        task.resume()
        return true
    }
    
    func processResponse(ptPath: PTPath, data: String) {
        struct Answer: Decodable {
            var status: String
            var error_msg: String?
        }

        let jsonData = data.data(using: .utf8)!
        let answer: Answer
        do {
            answer = try JSONDecoder().decode(Answer.self, from: jsonData)
        } catch {
            print("Error during decoding server answer: \(error)")
            return
        }
        
        if answer.status == "ok" {
            ptPath.sent = true
        } else {
            print("Error response from server: \(answer.error_msg!)")
        }
        
        do {
            try self.sendDB.privateMOC.save()
        } catch {
            print("Error saving path send state: \(error)")
        }
    }
    
    func pathToSend(context: NSManagedObjectContext) -> [PTPath] {
        let paths = PTPath.selectByActualUser(manageObjectContext: context)
        return paths.filter{!$0.sent}
    }
    
    func showSendAllResults() {
        db.mainMOC.refreshAllObjects()
        let success = pathToSend(context: db.mainMOC).count == 0
        let title = success ? "Upload Successfull" : "Upload Failed"
        let msg = success ? "All paths were successfully uploaded." : "Not all routes were uploaded."
        let alert = UIAlertController(title: title, message: msg, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "OK", style: .default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    }
    
    func test_setSentState(isSent: Bool) {
        for path in paths {
            path.sent = isSent
        }
        try! db.mainMOC.save()
    }
    
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
