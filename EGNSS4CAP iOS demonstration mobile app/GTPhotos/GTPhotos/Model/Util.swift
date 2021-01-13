import Foundation
import UIKit

@objc class ClosureSleeve: NSObject {
    let closure: ()->()

    init (_ closure: @escaping ()->()) {
        self.closure = closure
    }

    @objc func invoke () {
        closure()
    }
}

class Util {
    
    static func rad2deg(rads: Double) -> Double {
        return rads * 180 / .pi
    }
    
    static func deg2rad(degs: Double) -> Double {
        return degs * .pi / 180
    }
    
    static func screenOrientationToExif(screenOrientation: UIInterfaceOrientation) -> Int {
        switch screenOrientation {
        case .landscapeLeft:
            return 6
        case .landscapeRight:
            return 8
        case .portrait:
            return 1
        case.portraitUpsideDown:
            return 180
        default:
            return 0
        }
    }
    
    static func prettyDate(date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }
    
    static func prettyTime(date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        return formatter.string(from: date)
    }
    
    static func CIImageToCGImage(image: CIImage) -> CGImage {
        let context = CIContext(options: nil)
        return context.createCGImage(image, from: image.extent)!
    }
    
    static func UIImageToCIImage(image: UIImage) -> CIImage {
        return CIImage(image: image)!
    }
    
    static func scaleCGRectToCenter(origSize: CGSize, dest: CGRect) -> CGRect  {
        let oHeight = origSize.height
        let oWidth = origSize.width
        let aspect = oWidth / oHeight
        let land = oWidth > oHeight
        var newHeight: CGFloat
        var newWidth: CGFloat
        if land {
            newWidth = dest.size.width
            newHeight = newWidth / aspect
        } else {
            newHeight = dest.size.height
            newWidth = newHeight * aspect
        }
        let center = CGPoint(x: (dest.size.width / 2) + dest.origin.x, y: (dest.size.height / 2) + dest.origin.y)
        let orig = CGPoint(x: center.x - (newWidth / 2), y: center.y - (newHeight / 2))
        let scaleRect = CGRect(x: orig.x, y: orig.y, width: newWidth, height: newHeight)
        return scaleRect
    }
    
    static func degreeCycle(degree: Double) -> Double {
        var rem = degree.truncatingRemainder(dividingBy: 360)
        if (rem < 0) {
            rem += 360
        }
        return rem
    }
    
    static func encodeParameters(params: [String: String]) -> String {
        let queryItems = params.map{URLQueryItem(name:$0, value:$1)}
        var components = URLComponents()
        components.queryItems = queryItems
        return components.percentEncodedQuery ?? ""
    }
}
