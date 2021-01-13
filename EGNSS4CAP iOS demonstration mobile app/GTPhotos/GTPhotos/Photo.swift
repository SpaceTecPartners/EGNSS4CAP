//
//  Photo.swift
//  EGNSS4CAP
//
//  Created by FoxCom on 04/11/2020.
//

import UIKit

class Photo: NSObject, NSCoding {
    var lat:Float
    var lng:Float
    var created:Date
    var sended:Bool
    var note:String
    var photo:UIImage?
    
    struct PropertyKey {
        static let lat = "lat"
        static let lng = "lng"
        static let created = "created"
        static let sended = "sended"
        static let note = "note"
        static let photo = "photo"
    }
    
    //static var supportsSecureCoding: Bool = true
    static let DocumentsDirectory = FileManager().urls(for: .documentDirectory, in: .userDomainMask).first!
    static let ArchiveURL = DocumentsDirectory.appendingPathComponent("photos")
    
    required convenience init?(coder aDecoder: NSCoder) {
        
        guard let lat = aDecoder.decodeObject(forKey: PropertyKey.lat) as? Float else {
            return nil
        }
        guard let lng = aDecoder.decodeObject(forKey: PropertyKey.lng) as? Float else {
            return nil
        }
        guard let created = aDecoder.decodeObject(forKey: PropertyKey.created) as? Date else {
            return nil
        }
        guard let sended = aDecoder.decodeObject(forKey: PropertyKey.sended) as? Bool else {
            return nil
        }
        guard let note = aDecoder.decodeObject(forKey: PropertyKey.note) as? String else {
            return nil
        }
        let photo = aDecoder.decodeObject(forKey: PropertyKey.photo) as? UIImage

        self.init(lat: lat, lng: lng, created: created, sended: sended, note: note, photo: photo)
    }
    
    init(lat: Float, lng: Float, created: Date, sended: Bool, note: String, photo: UIImage?) {
        
        // Initialize stored properties.
        self.lat = lat
        self.lng = lng
        self.created = created
        self.sended = sended
        self.note = note
        self.photo = photo
    }
    
    func encode(with aCoder: NSCoder) {
        aCoder.encode(lat, forKey: PropertyKey.lat)
        aCoder.encode(lng, forKey: PropertyKey.lng)
        aCoder.encode(created, forKey: PropertyKey.created)
        aCoder.encode(sended, forKey: PropertyKey.sended)
        aCoder.encode(note, forKey: PropertyKey.note)
        aCoder.encode(photo, forKey: PropertyKey.photo)
    }
}
