import MapKit

class AzimuthMKAnnotationView: MKAnnotationView {
    
    override var annotation: MKAnnotation? {
        willSet {
            guard let annotation = newValue as? AzimuthMKAnnotation else {
                return
            }
            
            canShowCallout = false
            image = createIcon(angle: annotation.photo.photoHeading)
        }
    }
    
    private func createIcon(angle: Double) ->UIImage {
        let icon = #imageLiteral(resourceName: "icon_azimuth")
        let frame = CGRect(origin: CGPoint(x: 0, y: 0), size: CGSize(width: 100, height: 100))
        let azimFrame = CGRect(origin: CGPoint(x: 0, y: 50), size: CGSize(width: 100, height: 50))
        let renderer = UIGraphicsImageRenderer(size: frame.size)
        let image = renderer.image(actions: { context in
            context.cgContext.translateBy(x: 0, y: frame.size.height)
            context.cgContext.scaleBy(x: 1, y: -1)
            context.cgContext.draw(icon.cgImage!, in: Util.scaleCGRectToCenter(origSize: CGSize(width: 50, height: 50), dest: azimFrame))
        })
        return image.rotate(degs: angle)!
    }
}

// Created for the GSA in 2020-2021. Project management: SpaceTec Partners, software development: www.foxcom.eu
