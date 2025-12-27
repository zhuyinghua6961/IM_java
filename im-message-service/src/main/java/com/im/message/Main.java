package com.im.message;

import java.util.Arrays;

class Solution {
    public static int subarraySum(int[] nums, int k) {
        int res = 0;
        int sum = 0;
        for(int i = 0;i < nums.length;i++){
            sum = nums[i];
            if(sum == k){
                res++;
                break;
            }
            for(int j = i + 1;j < nums.length;j++){
                sum = sum + nums[j];
                if(sum == k){
                    res++;
                }else if(sum > k){
                    break;
                }
            }
        }
        return res;
    }

    public static void main(String[] args) {
        int[] nums = {8,54,7,-70,22,65,-6};
        subarraySum(nums,3);
    }
}