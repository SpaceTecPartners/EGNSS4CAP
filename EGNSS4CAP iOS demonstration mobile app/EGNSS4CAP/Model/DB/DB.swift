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

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
