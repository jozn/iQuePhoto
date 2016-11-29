package net.iquesoft.iquephoto.core.editor.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import net.iquesoft.iquephoto.util.RectUtil;

// TODO: Add opacity handle for stickers.
// TODO: Make abstract class for this and EditorText.
public class EditorSticker {
    private static final float MIN_SCALE = 0.15f;
    private static final int HELP_BOX_PAD = 25;
    private static final int BUTTON_WIDTH = 30;

    private float mRotateAngle = 0;
    private float initWidth;

    private boolean mIsDrawHelperFrame = true;

    public Bitmap mBitmap;

    private Paint mHelperFramePaint;

    private Matrix mMatrix;

    private RectF mImageRect;

    private Rect mSrcRect;
    private RectF mDstRect;

    private RectF mFrameRect;

    private Rect mDeleteHandleSrcRect;
    private Rect mFrontHandleSrcRect;
    private Rect mTransparencyHandleSrcRect;
    private Rect mResizeAndScaleHandleSrcRect;

    private RectF mDeleteHandleDstRect;
    private RectF mFrontHandleDstRect;
    private RectF mTransparencyHandleDstRect;
    private RectF mResizeAndScaleHandleDstRect;

    private EditorFrame mEditorFrame;

    public EditorSticker(Bitmap bitmap, RectF imageRect, EditorFrame editorFrame) {
        mBitmap = bitmap;

        mImageRect = imageRect;
        mEditorFrame = editorFrame;

        mHelperFramePaint = new Paint(editorFrame.getFramePaint());

        initialize();
    }

    public void initialize() {
        mSrcRect = new Rect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());

        int stickerWidth = Math.min(mBitmap.getWidth(), (int) mImageRect.width() >> 1);
        int stickerHeight = stickerWidth * mBitmap.getHeight() / mBitmap.getWidth();

        float left = mImageRect.centerX() - (stickerWidth / 2);
        float top = mImageRect.centerY() - (stickerHeight / 2);

        mDstRect = new RectF(left, top, left + stickerWidth, top + stickerHeight);

        mMatrix = new Matrix();
        mMatrix.postTranslate(mDstRect.left, mDstRect.top);
        mMatrix.postScale((float) stickerWidth / mBitmap.getWidth(),
                (float) stickerHeight / mBitmap.getHeight(), mDstRect.left,
                mDstRect.top);

        initWidth = mDstRect.width();

        mFrameRect = new RectF(mDstRect);
        updateFrameRect();

        mTransparencyHandleSrcRect = new Rect(0, 0, mEditorFrame.getDeleteHandleBitmap().getWidth(),
                mEditorFrame.getDeleteHandleBitmap().getHeight());
        mDeleteHandleSrcRect = new Rect(0, 0, mEditorFrame.getResizeHandleBitmap().getWidth(),
                mEditorFrame.getResizeHandleBitmap().getHeight());
        mResizeAndScaleHandleSrcRect = new Rect(0, 0, mEditorFrame.getRotateHandleBitmap().getWidth(),
                mEditorFrame.getRotateHandleBitmap().getHeight());
        mFrontHandleSrcRect = new Rect(0, 0, mEditorFrame.getFrontHandleBitmap().getWidth(),
                mEditorFrame.getFrontHandleBitmap().getHeight());

        int handleHalfSize = mEditorFrame.getDeleteHandleBitmap().getWidth() / 2;

        mDeleteHandleDstRect = new RectF(0, 0, handleHalfSize << 1, handleHalfSize << 1);
        mResizeAndScaleHandleDstRect = new RectF(0, 0, handleHalfSize << 1, handleHalfSize << 1);
        mFrontHandleDstRect = new RectF(0, 0, handleHalfSize << 1, handleHalfSize << 1);
        mTransparencyHandleDstRect = new RectF(0, 0, handleHalfSize << 1, handleHalfSize << 1);
    }

    private void updateFrameRect() {
        mFrameRect.left -= HELP_BOX_PAD;
        mFrameRect.right += HELP_BOX_PAD;
        mFrameRect.top -= HELP_BOX_PAD;
        mFrameRect.bottom += HELP_BOX_PAD;
    }

    public void setHelperFrameOpacity() {
        mHelperFramePaint.setAlpha(255);
    }

    public void resetHelperFrameOpacity() {
        mHelperFramePaint.set(mEditorFrame.getFramePaint());
    }

    public void actionMove(float dx, float dy) {
        mMatrix.postTranslate(dx, dy);

        mDstRect.offset(dx, dy);

        mFrameRect.offset(dx, dy);
    }

    public void updateRotateAndScale(final float dx, final float dy) {
        float stickerCenterX = mDstRect.centerX();
        float stickerCenterY = mDstRect.centerY();

        float handleCenterX = mResizeAndScaleHandleDstRect.centerX();
        float handleCenterY = mResizeAndScaleHandleDstRect.centerY();

        float n_x = handleCenterX + dx;
        float n_y = handleCenterY + dy;

        float xa = handleCenterX - stickerCenterX;
        float ya = handleCenterY - stickerCenterY;

        float xb = n_x - stickerCenterX;
        float yb = n_y - stickerCenterY;

        float srcLen = (float) Math.sqrt(xa * xa + ya * ya);
        float curLen = (float) Math.sqrt(xb * xb + yb * yb);

        float scale = curLen / srcLen;

        float newWidth = mDstRect.width() * scale;
        if (newWidth / initWidth < MIN_SCALE) {
            return;
        }

        mMatrix.postScale(scale, scale, mDstRect.centerX(),
                mDstRect.centerY());

        RectUtil.scaleRect(mDstRect, scale);

        mFrameRect.set(mDstRect);
        updateFrameRect();

        mResizeAndScaleHandleDstRect.offsetTo(mFrameRect.right - BUTTON_WIDTH, mFrameRect.bottom
                - BUTTON_WIDTH);
        mDeleteHandleDstRect.offsetTo(mFrameRect.left - BUTTON_WIDTH, mFrameRect.top
                - BUTTON_WIDTH);

        double cos = (xa * xb + ya * yb) / (srcLen * curLen);
        if (cos > 1 || cos < -1)
            return;
        float angle = (float) Math.toDegrees(Math.acos(cos));

        float calMatrix = xa * yb - xb * ya;

        int flag = calMatrix > 0 ? 1 : -1;
        angle = flag * angle;

        mRotateAngle += angle;
        mMatrix.postRotate(angle, mDstRect.centerX(),
                mDstRect.centerY());

        RectUtil.rotateRect(mResizeAndScaleHandleDstRect, mDstRect.centerX(),
                mDstRect.centerY(), mRotateAngle);
        RectUtil.rotateRect(mDeleteHandleDstRect, mDstRect.centerX(),
                mDstRect.centerY(), mRotateAngle);
    }

    public void draw(@NonNull Canvas canvas) {
        canvas.drawBitmap(mBitmap, mMatrix, null);

        // TODO: Doesn't draw rect.
        canvas.drawRect(mDstRect, mEditorFrame.getFramePaint());

        if (mIsDrawHelperFrame) drawHelperFrame(canvas);
    }

    private void drawHelperFrame(Canvas canvas) {
        canvas.save();
        canvas.rotate(mRotateAngle, mFrameRect.centerX(), mFrameRect.centerY());

        canvas.drawRect(mFrameRect, mHelperFramePaint);
        canvas.restore();

        int offsetValue = ((int) mDeleteHandleDstRect.width()) >> 1;

        mDeleteHandleDstRect.offsetTo(
                mFrameRect.left - offsetValue,
                mFrameRect.top - offsetValue
        );

        mResizeAndScaleHandleDstRect.offsetTo(
                mFrameRect.right - offsetValue,
                mFrameRect.bottom - offsetValue
        );

        mTransparencyHandleDstRect.offsetTo(
                mFrameRect.right - offsetValue,
                mFrameRect.top - offsetValue
        );

        mFrontHandleDstRect.offsetTo(
                mFrameRect.left - offsetValue,
                mFrameRect.bottom - offsetValue
        );

        RectUtil.rotateRect(mDeleteHandleDstRect, mFrameRect.centerX(),
                mFrameRect.centerY(), mRotateAngle);

        RectUtil.rotateRect(mTransparencyHandleDstRect, mFrameRect.centerX(),
                mFrameRect.centerY(), mRotateAngle);

        RectUtil.rotateRect(mResizeAndScaleHandleDstRect, mFrameRect.centerX(),
                mFrameRect.centerY(), mRotateAngle);

        RectUtil.rotateRect(mFrontHandleDstRect, mFrameRect.centerX(),
                mFrameRect.centerY(), mRotateAngle);

        canvas.drawBitmap(mEditorFrame.getDeleteHandleBitmap(),
                mDeleteHandleSrcRect,
                mDeleteHandleDstRect,
                null);

        canvas.drawBitmap(mEditorFrame.getRotateHandleBitmap(),
                mTransparencyHandleSrcRect,
                mTransparencyHandleDstRect,
                null);

        canvas.drawBitmap(mEditorFrame.getResizeHandleBitmap(),
                mResizeAndScaleHandleSrcRect,
                mResizeAndScaleHandleDstRect,
                null);

        canvas.drawBitmap(mEditorFrame.getFrontHandleBitmap(),
                mFrontHandleSrcRect,
                mFrontHandleDstRect,
                null);
    }

    public boolean isInside(MotionEvent event) {
        return mDstRect.contains(event.getX(), event.getY());
    }

    public boolean isInDeleteHandleButton(MotionEvent event) {
        return mDeleteHandleDstRect.contains(event.getX(), event.getY());
    }

    public boolean isInResizeAndScaleHandleButton(MotionEvent event) {
        return mResizeAndScaleHandleDstRect.contains(event.getX(), event.getY());
    }

    public boolean isInFrontHandleButton(MotionEvent event) {
        return mFrontHandleDstRect.contains(event.getX(), event.getY());
    }

    // TODO: Stickers transparency.
    public boolean isInTransparencyHandleButton(MotionEvent event) {
        return mTransparencyHandleDstRect.contains(event.getX(), event.getY());
    }

    public void setDrawHelperFrame(boolean isDrawHelperFrame) {
        mIsDrawHelperFrame = isDrawHelperFrame;
    }

    public void prepareToDraw(@NonNull RectF bitmapRect, @NonNull Bitmap bitmap) {
        float scaleX = bitmap.getWidth() / mDstRect.width();
        float scaleY = bitmap.getHeight() / mDstRect.height();

        float distanceX = bitmapRect.left - mDstRect.left;
        float distanceY = bitmapRect.top - mDstRect.top;

        //mMatrix.postScale(scaleX, scaleY);
        mMatrix.setTranslate(distanceX, distanceY);

        mIsDrawHelperFrame = false;
    }
}
