//
//  PersistPhoto+CoreDataProperties.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 06/11/2020.
//
//

import Foundation
import CoreData


extension PersistPhoto {

    @nonobjc public class func persistPhotoFetchRequest() -> NSFetchRequest<PersistPhoto> {
        return NSFetchRequest<PersistPhoto>(entityName: "PersistPhoto")
    }

    @NSManaged public var lat: Float
    @NSManaged public var lng: Float
    @NSManaged public var created: Date?
    @NSManaged public var sended: Bool
    @NSManaged public var note: String?
    @NSManaged public var photo: Data?

}

extension PersistPhoto : Identifiable {

}
