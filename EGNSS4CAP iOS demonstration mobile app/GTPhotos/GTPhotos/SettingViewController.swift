import UIKit

class SettingViewController: UIViewController, UITableViewDelegate, UITableViewDataSource {
    
    
    @IBOutlet weak var tableView: UITableView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        	
        tableView.delegate = self
        tableView.dataSource = self
        tableView.tableFooterView = UIView()
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return SESection.allCases.count
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return SESection(rawValue: section) == nil ? 0 : SESection(rawValue: section)!.optionCount
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let option = SEOption.init(rawValue: indexPath.row)!
        let cell = tableView.dequeueReusableCell(withIdentifier: option.type.reuseIdentifier, for: indexPath)
        let seCell = cell as! SECell
        seCell.reuse(option: option)
        return cell
    }
    

}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
