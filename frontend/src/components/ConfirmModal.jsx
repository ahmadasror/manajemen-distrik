import { Modal, Input, Form } from 'antd';
import { useState } from 'react';

export default function ConfirmModal({ open, title, message, onConfirm, onCancel, loading, showRemarks = false }) {
  const [remarks, setRemarks] = useState('');

  const handleOk = () => {
    onConfirm(showRemarks ? remarks : undefined);
    setRemarks('');
  };

  const handleCancel = () => {
    setRemarks('');
    onCancel();
  };

  return (
    <Modal
      open={open}
      title={title}
      onOk={handleOk}
      onCancel={handleCancel}
      confirmLoading={loading}
      okText="Confirm"
      cancelText="Cancel"
    >
      <p>{message}</p>
      {showRemarks && (
        <Form.Item label="Remarks">
          <Input.TextArea
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
            rows={3}
            placeholder="Enter remarks (optional)"
          />
        </Form.Item>
      )}
    </Modal>
  );
}
