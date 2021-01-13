import CoreData
import UIKit

class DB {
    
    lazy var mainMOC: NSManagedObjectContext = {
        return AppDelegate.gain().persistentContainer.viewContext
    }()
    
    lazy var privateMOC:NSManagedObjectContext = {
        return AppDelegate.gain().persistentContainer.newBackgroundContext()
    }()
}

