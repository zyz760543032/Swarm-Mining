public class TrajectroyException extends Exception {
    TrajectroyException(int[] nums){
        System.out.println("There are some errors near ");
        for (int num : nums) {
            System.out.print(num + " ");
        }
        System.out.println();
        System.out.println(" tuples");
    }
}
