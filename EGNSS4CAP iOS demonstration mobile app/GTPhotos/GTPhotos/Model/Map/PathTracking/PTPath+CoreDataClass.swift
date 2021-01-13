//
//  PTPath+CoreDataClass.swift
//  GTPhotos
//
//  Created by Jiří Müller on 27/11/2020.
//
//

import Foundation
import CoreData

@objc(PTPath)
public class PTPath: NSManagedObject {
    
    static func selectByActualUser(manageObjectContext: NSManagedObjectContext) -> [PTPath] {
        var paths: [PTPath] = []
        let pathRequest: NSFetchRequest<PTPath> = PTPath.fetchRequest()
        pathRequest.predicate = NSPredicate(format: "userId == %@ and end != nil", String(UserStorage.userID))
        let sort = NSSortDescriptor(key: #keyPath(PTPath.start), ascending: true)
        pathRequest.sortDescriptors = [sort]
        do {
            paths = try manageObjectContext.fetch(pathRequest)
        }
        catch {
            print("Could not load save data: \(error.localizedDescription)")
        }
        return paths
    }
}
