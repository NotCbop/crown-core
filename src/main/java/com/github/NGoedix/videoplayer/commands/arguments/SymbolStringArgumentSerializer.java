package com.github.NGoedix.videoplayer.commands.arguments;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;

public class SymbolStringArgumentSerializer implements ArgumentTypeInfo<SymbolStringArgumentType, SymbolStringArgumentSerializer.Template> {

   public void serializeToNetwork(Template pTemplate, FriendlyByteBuf pBuffer) {
   }

   public Template deserializeFromNetwork(FriendlyByteBuf pBuffer) {
      return new Template();
   }

   public void serializeToJson(Template pTemplate, JsonObject pJson) {
   }

   public Template unpack(SymbolStringArgumentType pArgument) {
      return new Template();
   }

   public final class Template implements ArgumentTypeInfo.Template<SymbolStringArgumentType> {

      public SymbolStringArgumentType instantiate(CommandBuildContext pContext) {
         return SymbolStringArgumentType.symbolString();
      }

      public ArgumentTypeInfo<SymbolStringArgumentType, ?> type() {
         return SymbolStringArgumentSerializer.this;
      }
   }
}