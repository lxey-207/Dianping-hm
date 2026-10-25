package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;


@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService iSeckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;

    @Override
    @Transactional
    public Result seckillVoucher(Long voucherId) {
        // 1. 查询优惠券
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        // 2. 判断秒杀是否开始
        LocalDateTime beginTime = voucher.getBeginTime();
        if (beginTime.isAfter(LocalDateTime.now())) {   // 未开始
            return Result.fail("秒杀未开始！");
        }
        // 3. 判断秒杀是否已经结束
        LocalDateTime endTime = voucher.getEndTime();
        if (endTime.isBefore(LocalDateTime.now())) {    // 已结束
            return Result.fail("秒杀已结束！");
        }
        // 4. 判断库存是否充足
        int stock = voucher.getStock();
        if (stock < 1) {
            return Result.fail("库存不足！");
        }
        // 5. 扣减库存
        iSeckillVoucherService
                .update().setSql("stock = stock - 1")
                .eq("voucher_id", voucherId).update();
        // 6. 创建订单
        VoucherOrder order = new VoucherOrder();

        long orderId = redisIdWorker.nextId("order:");
        order.setId(orderId);

        Long userId = UserHolder.getUser().getId();
        order.setUserId(userId);

        order.setVoucherId(voucherId);

        save(order);
        // 7. 返回订单id
        return Result.ok(orderId);
    }
}
