//
//  LoginViewController.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 03/11/2020.
//

import UIKit

class LoginViewController: UIViewController,UITextFieldDelegate {   

    @IBOutlet weak var backView: UIView!
    @IBOutlet weak var loginTextField: UITextField!
    @IBOutlet weak var pswdTextField: UITextField!
    
    override func viewDidLoad() {
        super.viewDidLoad()

        // Do any additional setup after loading the view.
        self.isModalInPresentation = true
        
        backView.layer.cornerRadius = 10
        backView.layer.shadowColor = UIColor.black.cgColor
        backView.layer.shadowOffset = CGSize(width: 3, height: 3)
        backView.layer.shadowOpacity = 0.3
        backView.layer.shadowRadius = 2.0
        
    }
    
    override func viewWillAppear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.portrait, andRotateTo: UIInterfaceOrientation.portrait)
    }
    
    override func viewWillDisappear(_ animated: Bool) {
        AppDelegate.AppUtility.lockOrientation(UIInterfaceOrientationMask.all)
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        
    }
    
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        switch textField {
        case loginTextField:
            pswdTextField.becomeFirstResponder()
        case pswdTextField:
            pswdTextField.resignFirstResponder()
            checkLogin()
        default:
            textField.resignFirstResponder()
        }
        return true
    }
    
    @IBAction func Login(_ sender: UIButton) {
        checkLogin()
    }
    
    func showConnError() {
        let alert = UIAlertController(title: "Login error", message: "Connection error", preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "Ok", style: UIAlertAction.Style.default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    }
    
    func showLoginError() {
        let alert = UIAlertController(title: "Login error", message: "Bad login or password", preferredStyle: UIAlertController.Style.alert)
        alert.addAction(UIAlertAction(title: "Ok", style: UIAlertAction.Style.default, handler: nil))
        self.present(alert, animated: true, completion: nil)
    }
    
    func processResponseData(data:String) {
        //print("Response data string:\n \(data)")
        struct Answer: Decodable {
            var status: String
            var error_msg: String?
            var user: Answer_user?
        }
        
        struct Answer_user: Decodable {
            var id: String
            var name: String
            var surname: String
        }

        let jsonData = data.data(using: .utf8)!
        let answer = try! JSONDecoder().decode(Answer.self, from: jsonData)
        
        if answer.status == "ok" {
            UserStorage.userID = Int((answer.user?.id)!)!
            UserStorage.login = loginTextField.text ?? ""
            UserStorage.userName = (answer.user?.name)!
            UserStorage.userSurname = (answer.user?.surname)!       
                
            performSegue(withIdentifier: "unwindToMainView", sender: self)
        } else {
            showLoginError()
        }
    }
    
    func checkLogin() {
        let loginText = loginTextField.text ?? ""
        let pswdText = pswdTextField.text ?? ""
        
        // Prepare URL
        let url = URL(string: "https://username:password@server/ws/comm_login.php")
        guard let requestUrl = url else { fatalError() }
        // Prepare URL Request Object
        var request = URLRequest(url: requestUrl)
        request.httpMethod = "POST"
         
        // HTTP Request Parameters which will be sent in HTTP Request Body
        let postString = "login="+loginText+"&pswd="+pswdText
        // Set HTTP Request Body
        request.httpBody = postString.data(using: String.Encoding.utf8);
        // Perform HTTP Request
        let task = URLSession.shared.dataTask(with: request) { (data, response, error) in
            // Check for Error
            if error != nil {
                DispatchQueue.main.async {
                    self.showConnError()
                }
                return
            }
            // Convert HTTP Response Data to a String
            if let data = data, let dataString = String(data: data, encoding: .utf8) {
                DispatchQueue.main.async {
                    self.processResponseData(data: dataString)
                }
            }
        }
        task.resume()
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
