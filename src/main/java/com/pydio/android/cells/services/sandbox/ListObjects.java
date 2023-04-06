package com.pydio.android.cells.services.sandbox;

class ListObjects {
}


//
//public class ListObjects {
//
//    public static void main(String[] args) {
//
//        final String usage = "\n" +
//                "Usage:\n" +
//                "    <bucketName> \n\n" +
//                "Where:\n" +
//                "    bucketName - The Amazon S3 bucket from which objects are read. \n\n";
//
//        if (args.length != 1) {
//            System.out.println(usage);
//            System.exit(1);
//        }
//
//        String bucketName = args[0];
//        ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.create();
//        Region region = Region.US_EAST_1;
//        S3Client s3 = S3Client.builder()
//                .region(region)
//                .credentialsProvider(credentialsProvider)
//                .build();
//
//        listBucketObjects(s3, bucketName);
//        s3.close();
//    }
//
//    // snippet-start:[s3.java2.list_objects.main]
//    public static void listBucketObjects(S3Client s3, String bucketName) {
//
//        try {
//            ListObjectsRequest listObjects = ListObjectsRequest
//                    .builder()
//                    .bucket(bucketName)
//                    .build();
//
//            ListObjectsResponse res = s3.listObjects(listObjects);
//            List<S3Object> objects = res.contents();
//            for (S3Object myValue : objects) {
//                System.out.print("\n The name of the key is " + myValue.key());
//                System.out.print("\n The object is " + calKb(myValue.size()) + " KBs");
//                System.out.print("\n The owner is " + myValue.owner());
//            }
//
//        } catch (S3Exception e) {
//            System.err.println(e.awsErrorDetails().errorMessage());
//            System.exit(1);
//        }
//    }
//
//    //convert bytes to kbs.
//    private static long calKb(Long val) {
//        return val / 1024;
//    }
//    // snippet-end:[s3.java2.list_objects.main]
//}