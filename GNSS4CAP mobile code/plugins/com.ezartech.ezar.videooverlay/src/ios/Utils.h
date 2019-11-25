#import <Foundation/Foundation.h>

//example use
//  NSString *actualStr = [NSString stringWithFormat:@"%@",@"given string which need to convert into base64"];
//  NSData   *convertedData = [actualStr dataUsingEncoding:NSUTF8StringEncoding];
//  NSString *convertedBase64Val; = [NSString stringWithFormat:@"%@",[Base64 encode:convertedData]];

@interface Base64 : NSObject
{
}
+ (void)initialize;
+ (NSString *)encode:(const uint8_t *)input length:(NSInteger)length;
+ (NSString *)encode:(NSData *)rawBytes;
+ (NSData *)decode:(const char *)string length:(NSInteger)inputLength;
+ (NSData *)decode:(NSString *)string;

+ (NSString *)encode:(NSData *)rawBytes as;
@end
