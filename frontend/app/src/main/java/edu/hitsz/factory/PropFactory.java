package edu.hitsz.factory;

import edu.hitsz.prop.AbstractProp;

/**
 - 道具工厂接口
 */
public interface PropFactory {
    /**
     - 创建道具对象
     - @param locationX 道具位置x坐标
     - @param locationY 道具位置y坐标
     - @return 道具对象
     */
    AbstractProp createProp(int locationX, int locationY);
}
