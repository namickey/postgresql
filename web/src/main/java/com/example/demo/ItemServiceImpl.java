package com.example.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItemServiceImpl implements ItemService {

    Logger looger = LoggerFactory.getLogger(ItemServiceImpl.class);

    @Autowired
    private ItemMapper itemMapper;

    @Override
    public String getItem(String id) {
        looger.info("getItem called.");
        Item item = new Item();
        item.setId(id);
        Item ret = itemMapper.search(item);
        if (ret == null) {
            return "";
        }
        return ret.getName();
    }
}
